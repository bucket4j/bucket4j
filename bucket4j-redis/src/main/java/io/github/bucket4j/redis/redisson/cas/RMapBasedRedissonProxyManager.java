/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.redis.redisson.cas;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import org.redisson.api.RFuture;
import org.redisson.api.RMap;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RMapBasedRedissonProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final RMap<K, byte[]> buckets;

    public RMapBasedRedissonProxyManager(RMap<K, byte[]> buckets, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.buckets = buckets;
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                byte[] persistedState = buckets.get(key);
                return Optional.ofNullable(persistedState);
            }
            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData) {
                if (originalData == null) {
                    // Redisson prohibits the usage null as values, so "replace" must not be used in such cases
                    return buckets.putIfAbsent(key, newData) == null;
                } else {
                    return buckets.replace(key, originalData, newData);
                }
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData() {
                RFuture<byte[]> redissonFuture = buckets.getAsync(key);
                return convertFuture(redissonFuture)
                    .thenApply((byte[] resultBytes) -> Optional.ofNullable(resultBytes));
            }
            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData) {
                if (originalData == null) {
                    // Redisson prohibits the usage null as values, so "replace" must not be used in such cases
                    RFuture<byte[]> redissonFuture = buckets.putIfAbsentAsync(key, newData);
                    return convertFuture(redissonFuture).thenApply(Objects::isNull);
                } else {
                    RFuture<Boolean> redissonFuture = buckets.replaceAsync(key, originalData, newData);
                    return convertFuture(redissonFuture);
                }
            }
        };
    }

    @Override
    public void removeProxy(K key) {
        buckets.remove(key);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        RFuture<byte[]> redissonFuture = buckets.removeAsync(key);
        return convertFuture(redissonFuture).thenApply(bytes -> null);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    private <T> CompletableFuture<T> convertFuture(RFuture<T> redissonFuture) {
        CompletableFuture<T> jdkFuture = new CompletableFuture<>();
        redissonFuture.whenComplete((result, error) -> {
           if (error != null) {
               jdkFuture.completeExceptionally(error);
           } else {
               jdkFuture.complete(result);
           }
        });
        return jdkFuture;
    }

}