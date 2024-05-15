/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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

package io.github.bucket4j.redis.lettuce.cas;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.redis.consts.LuaScripts;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.redis.lettuce.RedisApi;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;

public class LettuceBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final RedisApi<K> redisApi;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    public LettuceBasedProxyManager(Bucket4jLettuce.LettuceBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.expirationStrategy = builder.getExpirationAfterWrite().orElse(ExpirationAfterWriteStrategy.none());
        this.redisApi = builder.getRedisApi();
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        @SuppressWarnings("unchecked")
        K[] keys = (K[]) new Object[]{key};
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                RedisFuture<byte[]> stateFuture = redisApi.get(key);
                return Optional.ofNullable(getFutureValue(stateFuture, timeoutNanos));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                return getFutureValue(compareAndSwapFuture(keys, originalData, newData, newState), timeoutNanos);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        @SuppressWarnings("unchecked")
        K[] keys = (K[]) new Object[]{key};
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                RedisFuture<byte[]> stateFuture = redisApi.get(key);
                return convertToCompletableFuture(stateFuture, timeoutNanos)
                        .thenApply(Optional::ofNullable);
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                return convertToCompletableFuture(compareAndSwapFuture(keys, originalData, newData, newState), timeoutNanos);
            }
        };
    }

    @Override
    public void removeProxy(K key) {
        RedisFuture<?> future = redisApi.delete(key);
        getFutureValue(future, Optional.empty());
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        RedisFuture<?> future = redisApi.delete(key);
        return convertToCompletableFuture(future, Optional.empty()).thenApply(bytes -> null);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    private RedisFuture<Boolean> compareAndSwapFuture(K[] keys, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
        if (ttlMillis > 0) {
            if (originalData == null) {
                // nulls are prohibited as values, so "replace" must not be used in such cases
                byte[][] params = {newData, encodeLong(ttlMillis)};
                return redisApi.eval(LuaScripts.SCRIPT_SET_NX_PX, ScriptOutputType.BOOLEAN, keys, params);
            } else {
                byte[][] params = {originalData, newData, encodeLong(ttlMillis)};
                return redisApi.eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP_PX, ScriptOutputType.BOOLEAN, keys, params);
            }
        } else {
            if (originalData == null) {
                // nulls are prohibited as values, so "replace" must not be used in such cases
                byte[][] params = {newData};
                return redisApi.eval(LuaScripts.SCRIPT_SET_NX, ScriptOutputType.BOOLEAN, keys, params);
            } else {
                byte[][] params = {originalData, newData};
                return redisApi.eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP, ScriptOutputType.BOOLEAN, keys, params);
            }
        }
    }

    private <T> CompletableFuture<T> convertToCompletableFuture(RedisFuture<T> redisFuture, Optional<Long> timeoutNanos) {
        if (timeoutNanos.isEmpty()) {
            return redisFuture.toCompletableFuture();
        } else {
            return redisFuture.toCompletableFuture().orTimeout(timeoutNanos.get(), TimeUnit.NANOSECONDS);
        }
    }

    private <V> V getFutureValue(RedisFuture<V> redisFuture, Optional<Long> timeoutNanos) {
        try {
            if (timeoutNanos.isEmpty()) {
                return redisFuture.get();
            } else {
                return redisFuture.get(timeoutNanos.get(), TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException e) {
            redisFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw new RedisException(e);
        } catch (java.util.concurrent.TimeoutException e) {
            String message = "Violated timeout while waiting for redis future for " + timeoutNanos.get() + "ns";
            throw new io.github.bucket4j.TimeoutException(message, timeoutNanos.get(), timeoutNanos.get());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RedisException re) {
                throw re;
            }
            throw new RedisException("Unexpected exception while processing command", e.getCause());
        }
    }

    private byte[] encodeLong(Long value) {
        return ("" + value).getBytes(StandardCharsets.UTF_8);
    }

}