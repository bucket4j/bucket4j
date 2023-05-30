/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2021 Vladimir Bukhtoyarov
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

package io.github.bucket4j.grid.ignite.thin.cas;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.grid.ignite.thin.ThinClientUtils;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClientFuture;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class IgniteThinClientCasBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final ClientCache<K, ByteBuffer> cache;

    public IgniteThinClientCasBasedProxyManager(ClientCache<K, ByteBuffer> cache) {
        this(cache, ClientSideConfig.getDefault());
    }

    public IgniteThinClientCasBasedProxyManager(ClientCache<K, ByteBuffer> cache, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                ByteBuffer persistedState = cache.get(key);
                if (persistedState == null) {
                    return Optional.empty();
                }
                byte[] persistedStateBytes = persistedState.array();
                return Optional.of(persistedStateBytes);
            }
            @Override
            public boolean compareAndSwap(byte[] originalDataBytes, byte[] newDataBytes, RemoteBucketState newState) {
                ByteBuffer newData = ByteBuffer.wrap(newDataBytes);
                if (originalDataBytes == null) {
                    return cache.putIfAbsent(key, newData);
                }
                ByteBuffer originalData = ByteBuffer.wrap(originalDataBytes);
                return cache.replace(key, originalData, newData);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData() {
                IgniteClientFuture<ByteBuffer> igniteFuture = cache.getAsync(key);
                CompletableFuture<ByteBuffer> resultFuture = ThinClientUtils.convertFuture(igniteFuture);
                return resultFuture.thenApply((ByteBuffer persistedState) -> {
                    if (persistedState == null) {
                        return Optional.empty();
                    }
                    byte[] persistedStateBytes = persistedState.array();
                    return Optional.of(persistedStateBytes);
                });
            }
            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalDataBytes, byte[] newDataBytes, RemoteBucketState newState) {
                ByteBuffer newData = ByteBuffer.wrap(newDataBytes);
                if (originalDataBytes == null) {
                    IgniteClientFuture<Boolean> igniteFuture = cache.putIfAbsentAsync(key, newData);
                    return ThinClientUtils.convertFuture(igniteFuture);
                }
                ByteBuffer originalData = ByteBuffer.wrap(originalDataBytes);
                IgniteClientFuture<Boolean> igniteFuture = cache.replaceAsync(key, originalData, newData);
                return ThinClientUtils.convertFuture(igniteFuture);
            }
        };
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public void removeProxy(K key) {
        cache.remove(key);
    }

    @Override
    protected CompletableFuture<?> removeAsync(K key) {
        IgniteClientFuture<Boolean> igniteFuture = cache.removeAsync(key);
        return ThinClientUtils.convertFuture(igniteFuture);
    }

}
