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
package io.github.bucket4j.redis.vertx.cas;

import io.github.bucket4j.TimeoutException;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.consts.LuaScripts;
import io.github.bucket4j.redis.vertx.Bucket4jVertx;
import io.github.bucket4j.redis.vertx.RedisApi;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class VertxBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final RedisApi redisApi;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final Mapper<K> keyMapper;

    public VertxBasedProxyManager(Bucket4jVertx.VertxBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.redisApi = builder.getRedisApi();
        this.expirationStrategy = builder.getExpirationAfterWrite().orElse(ExpirationAfterWriteStrategy.none());
        this.keyMapper = builder.getKeyMapper();
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        byte[] keyBytes = keyMapper.toBytes(key);
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                return Optional.ofNullable(awaitResult(redisApi.get(keyBytes), timeoutNanos));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                return awaitResult(compareAndSwapFuture(keyBytes, originalData, newData, newState), timeoutNanos);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        byte[] keyBytes = keyMapper.toBytes(key);
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                return withTimeout(redisApi.get(keyBytes), timeoutNanos).thenApply(Optional::ofNullable);
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                return withTimeout(compareAndSwapFuture(keyBytes, originalData, newData, newState), timeoutNanos);
            }
        };
    }

    @Override
    public void removeProxy(K key) {
        awaitResult(redisApi.delete(keyMapper.toBytes(key)), Optional.empty());
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return redisApi.delete(keyMapper.toBytes(key));
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    private CompletableFuture<Boolean> compareAndSwapFuture(byte[] key, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
        byte[][] keys = new byte[][] {key};
        if (ttlMillis > 0) {
            if (originalData == null) {
                byte[][] params = new byte[][] {newData, encodeLong(ttlMillis)};
                return redisApi.eval(LuaScripts.SCRIPT_SET_NX_PX, keys, params);
            } else {
                byte[][] params = new byte[][] {originalData, newData, encodeLong(ttlMillis)};
                return redisApi.eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP_PX, keys, params);
            }
        } else {
            if (originalData == null) {
                byte[][] params = new byte[][] {newData};
                return redisApi.eval(LuaScripts.SCRIPT_SET_NX, keys, params);
            } else {
                byte[][] params = new byte[][] {originalData, newData};
                return redisApi.eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP, keys, params);
            }
        }
    }

    private byte[] encodeLong(long value) {
        return Long.toString(value).getBytes(StandardCharsets.UTF_8);
    }

    private <T> CompletableFuture<T> withTimeout(CompletableFuture<T> future, Optional<Long> timeoutNanos) {
        if (timeoutNanos.isEmpty()) {
            return future;
        }
        return future.orTimeout(timeoutNanos.get(), TimeUnit.NANOSECONDS);
    }

    private <T> T awaitResult(CompletableFuture<T> future, Optional<Long> timeoutNanos) {
        try {
            if (timeoutNanos.isEmpty()) {
                return future.get();
            }
            return future.get(timeoutNanos.get(), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Redis command", e);
        } catch (java.util.concurrent.TimeoutException e) {
            long timeout = timeoutNanos.orElse(0L);
            String message = "Violated timeout while waiting for Redis command result for " + timeout + "ns";
            throw new TimeoutException(message, timeout, timeout);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Unexpected exception while processing redis command", e.getCause());
        }
    }

}
