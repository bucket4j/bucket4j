/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.memcached.lock;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheStatus;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.LockBasedTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.memcached.Bucket4jMemcached;

/**
 * Lock-based proxy manager for Memcached, built on top of the <a href="https://github.com/spotify/folsom">folsom</a> client.
 *
 * <p>
 * Memcached has no native distributed locking primitive, but its {@code add} command (store a value only if the key
 * is currently absent) can be used to build one: a client "acquires" a bucket by {@code add}-ing a dedicated lock key,
 * and "releases" it by deleting that key. While the lock is held, the bucket state is read/updated with plain
 * {@code get}/{@code set}, which is cheaper than the compare-and-swap retry loop used by
 * {@link io.github.bucket4j.memcached.cas.MemcachedCompareAndSwapBasedProxyManager} under heavy contention on the same key.
 * The lock key carries its own TTL so that a client that crashes while holding it does not wedge the bucket forever.
 *
 * <p>
 * Lock acquisition inherently requires a blocking retry loop, so this proxy manager only supports the synchronous API
 * and uses folsom's futures by simply blocking on them.
 *
 * @param <K> the generic type for unique identifiers that used to point to the bucket in external storage.
 */
public class MemcachedLockBasedProxyManager<K> extends AbstractLockBasedProxyManager<K> {

    private static final byte[] LOCK_MARKER = {1};
    private static final String LOCK_KEY_SUFFIX = ".bucket4j-lock";

    private final MemcacheClient<byte[]> client;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final int lockExpirationSeconds;
    private final long lockPollPeriodMillis;

    public MemcachedLockBasedProxyManager(Bucket4jMemcached.MemcachedLockBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.client = builder.getClient();
        this.keyMapper = builder.getKeyMapper();
        this.expirationStrategy = builder.getClientSideConfig().getExpirationAfterWriteStrategy()
            .orElseGet(ExpirationAfterWriteStrategy::none);
        this.lockExpirationSeconds = builder.getLockExpirationSeconds();
        this.lockPollPeriodMillis = builder.getLockPollPeriodMillis();
    }

    @Override
    public void removeProxy(K key) {
        getFutureValue(client.delete(keyMapper.toString(key)), Optional.empty());
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    @Override
    protected LockBasedTransaction allocateTransaction(K key, Optional<Long> timeoutNanos) {
        String dataKey = keyMapper.toString(key);
        String lockKey = dataKey + LOCK_KEY_SUFFIX;
        return new LockBasedTransaction() {

            private volatile boolean lockOwnedByThisTransaction;

            @Override
            public void begin(Optional<Long> timeoutNanos) {
                // do nothing, memcached does not provide transactions
            }

            @Override
            public void rollback() {
                // do nothing, there is nothing to rollback beyond releasing the lock which happens in unlock()
            }

            @Override
            public void commit(Optional<Long> timeoutNanos) {
                // do nothing, every write is already durable once set()/add() returns
            }

            @Override
            public byte[] lockAndGet(Optional<Long> timeoutNanos) {
                acquireLock(timeoutNanos);
                return getFutureValue(client.get(dataKey), timeoutNanos);
            }

            @Override
            public void unlock() {
                if (!lockOwnedByThisTransaction) {
                    return;
                }
                lockOwnedByThisTransaction = false;
                getFutureValue(client.delete(lockKey), Optional.empty());
            }

            @Override
            public void create(byte[] data, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                save(data, newState, timeoutNanos);
            }

            @Override
            public void update(byte[] data, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                save(data, newState, timeoutNanos);
            }

            @Override
            public void release() {
                // do nothing, unlock() already released the mutex
            }

            private void acquireLock(Optional<Long> timeoutNanos) {
                long deadlineNanos = timeoutNanos.map(t -> System.nanoTime() + t).orElse(Long.MAX_VALUE);
                while (true) {
                    MemcacheStatus status = getFutureValue(client.add(lockKey, LOCK_MARKER, lockExpirationSeconds), timeoutNanos);
                    if (status == MemcacheStatus.OK) {
                        lockOwnedByThisTransaction = true;
                        return;
                    }
                    if (System.nanoTime() >= deadlineNanos) {
                        throw BucketExceptions.from(new java.util.concurrent.TimeoutException("Timed out while waiting for memcached lock " + lockKey));
                    }
                    try {
                        Thread.sleep(lockPollPeriodMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw BucketExceptions.from(e);
                    }
                }
            }

            private void save(byte[] data, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                int expirationSeconds = expirationSeconds(newState);
                getFutureValue(client.set(dataKey, data, expirationSeconds), timeoutNanos);
            }
        };
    }

    private int expirationSeconds(RemoteBucketState newState) {
        long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
        if (ttlMillis <= 0) {
            // 0 means "never expire" for memcached
            return 0;
        }
        return (int) Math.max(1, TimeUnit.MILLISECONDS.toSeconds(ttlMillis));
    }

    private <T> T getFutureValue(CompletionStage<T> stage, Optional<Long> timeoutNanos) {
        CompletableFuture<T> future = stage.toCompletableFuture();
        try {
            if (timeoutNanos.isEmpty()) {
                return future.get();
            } else {
                return future.get(timeoutNanos.get(), TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw BucketExceptions.from(e);
        } catch (TimeoutException e) {
            throw new io.github.bucket4j.TimeoutException("Violated timeout while waiting for memcached future", timeoutNanos.get(), timeoutNanos.get());
        } catch (ExecutionException e) {
            throw BucketExceptions.from(e.getCause());
        }
    }

}
