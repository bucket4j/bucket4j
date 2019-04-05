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

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedBackend;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapBasedTransaction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CompareAndSwapBasedBackendMock<K extends Serializable> extends AbstractCompareAndSwapBasedBackend<K> {

    private final Map<K, byte[]> stateMap = new HashMap<>();

    public CompareAndSwapBasedBackendMock(TimeMeter timeMeter) {
        super(timeMeter);
    }

    @Override
    protected CompareAndSwapBasedTransaction allocateTransaction(K key) {
        byte[] backup = stateMap.get(key);
        return new CompareAndSwapBasedTransaction() {
            @Override
            public void begin() {
                // do nothing
            }

            @Override
            public Optional<byte[]> get() {
                return Optional.ofNullable(backup);
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData) {
                stateMap.put(key, newData);
                return true;
            }

            @Override
            public void rollback() {
                stateMap.put(key, backup);
            }

            @Override
            public void commit() {
                // do nothing
            }
        };
    }

    @Override
    protected void releaseTransaction(CompareAndSwapBasedTransaction transaction) {
        // do nothing
    }

}
