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
import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractSelectForUpdateBasedBackend;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.SelectForUpdateBasedTransaction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SelectForUpdateBasedBackendMock<K extends Serializable> extends AbstractSelectForUpdateBasedBackend<K> {

    private final Map<K, byte[]> stateMap = new HashMap<>();

    public SelectForUpdateBasedBackendMock(TimeMeter timeMeter) {
        super(timeMeter);
    }

    @Override
    protected SelectForUpdateBasedTransaction allocateTransaction(K key) {
        byte[] backup = stateMap.get(key);

        return new SelectForUpdateBasedTransaction() {
            @Override
            public void begin() {
                // do nothing
            }

            @Override
            public Optional<byte[]> lockAndGet() {
                return Optional.ofNullable(backup);
            }

            @Override
            public void update(byte[] data) {
                stateMap.put(key, data);
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
    protected void releaseTransaction(SelectForUpdateBasedTransaction transaction) {
        // do nothing
    }

}
