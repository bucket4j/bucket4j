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
package io.github.bucket4j.memcached.cas;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.spotify.folsom.GetResult;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheStatus;

import io.github.bucket4j.TimeoutException;
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.memcached.Bucket4jMemcached;
import io.github.bucket4j.memcached.MemcachedExpirations;

/**
 * Compare-and-swap-based proxy manager for Memcached, built on top of the <a href="https://github.com/spotify/folsom">folsom</a>
 * client's native {@code casGet}/{@code set(key, value, ttl, cas)}/{@code add} operations.
 *
 * <p>
 * Memcached has no server-side scripting/RPC capability, so unlike Redis-based proxy managers this implementation
 * cannot execute the bucket state transition atomically on the server. Instead, the state transition is computed
 * on the client and persisted using memcached's native optimistic-locking primitive: a document is read together
 * with its CAS token via {@code gets}, and the new state is written back with {@code cas} only if nobody else
 * modified the document since it was read. If the CAS fails, the whole operation is retried from scratch.
 *
 * <p>
 * Because folsom's API is natively based on {@link java.util.concurrent.CompletionStage}, both the synchronous and
 * the asynchronous flavours of this proxy manager perform genuine non-blocking I/O against memcached; the synchronous
 * flavour merely blocks the calling thread until the underlying future completes.
 *
 * @param <K> the generic type for unique identifiers that used to point to the bucket in external storage.
 */
public class MemcachedCompareAndSwapBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final MemcacheClient<byte[]> client;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    public MemcachedCompareAndSwapBasedProxyManager(Bucket4jMemcached.MemcachedCompareAndSwapBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.client = builder.getClient();
        this.keyMapper = builder.getKeyMapper();
        this.expirationStrategy = builder.getClientSideConfig().getExpirationAfterWriteStrategy()
            .orElseGet(ExpirationAfterWriteStrategy::none);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        String memcachedKey = keyMapper.toString(key);
        return new CompareAndSwapOperation() {
            private volatile long currentCas;

            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                GetResult<byte[]> result = getFutureValue(client.casGet(memcachedKey), timeoutNanos);
                if (result == null) {
                    currentCas = 0L;
                    return Optional.empty();
                }
                currentCas = result.getCas();
                return Optional.of(result.getValue());
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                int expirationSeconds = expirationSeconds(newState);
                boolean isAdd = originalData == null;
                MemcacheStatus status;
                if (isAdd) {
                    // nulls are prohibited as values, so "add" (store-if-absent) must be used in such cases
                    status = getFutureValue(client.add(memcachedKey, newData, expirationSeconds), timeoutNanos);
                } else {
                    status = getFutureValue(client.set(memcachedKey, newData, expirationSeconds, currentCas), timeoutNanos);
                }
                return toCasResult(memcachedKey, status, isAdd);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        String memcachedKey = keyMapper.toString(key);
        return new AsyncCompareAndSwapOperation() {
            private volatile long currentCas;

            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                return withTimeout(client.casGet(memcachedKey).toCompletableFuture(), timeoutNanos)
                    .thenApply(result -> {
                        if (result == null) {
                            currentCas = 0L;
                            return Optional.empty();
                        }
                        currentCas = result.getCas();
                        return Optional.of(result.getValue());
                    });
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                int expirationSeconds = expirationSeconds(newState);
                boolean isAdd = originalData == null;
                CompletableFuture<MemcacheStatus> future = isAdd
                    // nulls are prohibited as values, so "add" (store-if-absent) must be used in such cases
                    ? client.add(memcachedKey, newData, expirationSeconds).toCompletableFuture()
                    : client.set(memcachedKey, newData, expirationSeconds, currentCas).toCompletableFuture();
                return withTimeout(future, timeoutNanos).thenApply(status -> toCasResult(memcachedKey, status, isAdd));
            }
        };
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return withTimeout(client.delete(keyMapper.toString(key)).toCompletableFuture(), Optional.empty())
            .thenApply(status -> null);
    }

    @Override
    public void removeProxy(K key) {
        getFutureValue(client.delete(keyMapper.toString(key)), Optional.empty());
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    private int expirationSeconds(RemoteBucketState newState) {
        long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
        return MemcachedExpirations.toMemcachedExpiration(ttlMillis);
    }

    /**
     * Interprets the outcome of an {@code add}/{@code cas} write. For {@code add}, a losing race reports
     * {@code KEY_EXISTS} over the binary protocol or {@code ITEM_NOT_STORED} over the ascii protocol; for
     * {@code cas}, a losing race reports {@code KEY_EXISTS} (value changed since it was read) or
     * {@code KEY_NOT_FOUND} (key concurrently deleted). All of these are ordinary CAS conflicts that the caller
     * retries. Any other non-{@code OK} status (e.g. {@code VALUE_TOO_LARGE}, {@code OUT_OF_MEMORY}) indicates a
     * permanent failure and must be propagated instead of being silently retried forever.
     */
    private boolean toCasResult(String memcachedKey, MemcacheStatus status, boolean isAdd) {
        if (status == MemcacheStatus.OK) {
            return true;
        }
        boolean isConflict = isAdd
            ? status == MemcacheStatus.KEY_EXISTS || status == MemcacheStatus.ITEM_NOT_STORED
            : status == MemcacheStatus.KEY_EXISTS || status == MemcacheStatus.KEY_NOT_FOUND;
        if (isConflict) {
            return false;
        }
        throw new BucketExceptions.BucketExecutionException(
            "Memcached CAS operation on key \"" + memcachedKey + "\" failed with status " + status);
    }

    private <T> CompletableFuture<T> withTimeout(CompletableFuture<T> future, Optional<Long> timeoutNanos) {
        if (timeoutNanos.isEmpty()) {
            return future;
        }
        return future.orTimeout(timeoutNanos.get(), TimeUnit.NANOSECONDS);
    }

    private <T> T getFutureValue(java.util.concurrent.CompletionStage<T> stage, Optional<Long> timeoutNanos) {
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
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Violated timeout while waiting for memcached future", timeoutNanos.get(), timeoutNanos.get());
        } catch (ExecutionException e) {
            throw BucketExceptions.from(e.getCause());
        }
    }

}
