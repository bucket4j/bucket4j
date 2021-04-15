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

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedBackend;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CompareAndSwapBasedBackendMock<K> extends AbstractCompareAndSwapBasedBackend<K> {

    private final Map<K, byte[]> stateMap = new HashMap<>();

    public CompareAndSwapBasedBackendMock(ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        byte[] backup = stateMap.get(key);
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                return Optional.ofNullable(backup);
            }
            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData) {
                stateMap.put(key, newData);
                return true;
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        byte[] backup = stateMap.get(key);
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData() {
                return CompletableFuture.completedFuture(Optional.ofNullable(backup));
            }
            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData) {
                stateMap.put(key, newData);
                return CompletableFuture.completedFuture(true);
            }
        };
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

}
