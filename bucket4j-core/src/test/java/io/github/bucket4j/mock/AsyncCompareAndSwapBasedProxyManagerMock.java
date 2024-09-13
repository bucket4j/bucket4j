/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.github.bucket4j.distributed.proxy.AsyncProxyManagerConfig;
import io.github.bucket4j.distributed.proxy.ProxyManagerConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractAsyncCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

public class AsyncCompareAndSwapBasedProxyManagerMock<K> extends AbstractAsyncCompareAndSwapBasedProxyManager<K> {

    private final Map<K, byte[]> stateMap = new HashMap<>();

    public AsyncCompareAndSwapBasedProxyManagerMock(AsyncProxyManagerConfig<K> proxyManagerConfig) {
        super(proxyManagerConfig);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        stateMap.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        byte[] backup = stateMap.get(key);
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                return CompletableFuture.completedFuture(Optional.ofNullable(backup));
            }
            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                stateMap.put(key, newData);
                return CompletableFuture.completedFuture(true);
            }
        };
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return false;
    }
}
