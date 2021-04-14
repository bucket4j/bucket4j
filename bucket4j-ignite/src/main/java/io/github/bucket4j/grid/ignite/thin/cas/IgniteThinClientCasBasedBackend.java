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

package io.github.bucket4j.grid.ignite.thin.cas;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedBackend;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapBasedTransaction;
import org.apache.ignite.client.ClientCache;

import java.util.Objects;
import java.util.Optional;

public class IgniteThinClientCasBasedBackend<K> extends AbstractCompareAndSwapBasedBackend<K> {

    private final ClientCache<K, byte[]> cache;

    public IgniteThinClientCasBasedBackend(ClientCache<K, byte[]> cache, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    protected CompareAndSwapBasedTransaction allocateTransaction(K key) {
        return new IgniteCompareAndSwapTransaction(key);
    }

    @Override
    protected void releaseTransaction(CompareAndSwapBasedTransaction transaction) {
        // do nothing
    }

    private class IgniteCompareAndSwapTransaction implements CompareAndSwapBasedTransaction {

        private final K key;

        private IgniteCompareAndSwapTransaction(K key) {
            this.key = key;
        }

        @Override
        public Optional<byte[]> get() {
            byte[] persistedState = cache.get(key);
            return Optional.ofNullable(persistedState);
        }

        @Override
        public boolean compareAndSwap(byte[] originalData, byte[] newData) {
            if (originalData == null) {
                return cache.putIfAbsent(key, newData);
            }
            return cache.replace(key, originalData, newData);
        }

    }

}
