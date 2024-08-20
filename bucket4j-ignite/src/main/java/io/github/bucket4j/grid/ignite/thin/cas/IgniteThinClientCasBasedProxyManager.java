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

import java.nio.ByteBuffer;
import java.util.Optional;

import org.apache.ignite.client.ClientCache;

import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.grid.ignite.thin.Bucket4jIgniteThin.IgniteThinClientCasBasedProxyManagerBuilder;

public class IgniteThinClientCasBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final ClientCache<K, ByteBuffer> cache;

    public IgniteThinClientCasBasedProxyManager(IgniteThinClientCasBasedProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        this.cache = builder.getCache();
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                ByteBuffer persistedState = cache.get(key);
                if (persistedState == null) {
                    return Optional.empty();
                }
                byte[] persistedStateBytes = persistedState.array();
                return Optional.of(persistedStateBytes);
            }
            @Override
            public boolean compareAndSwap(byte[] originalDataBytes, byte[] newDataBytes, RemoteBucketState newState, Optional<Long> timeoutNanos) {
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
    public void removeProxy(K key) {
        cache.remove(key);
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return false;
    }

}
