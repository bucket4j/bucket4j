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
import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractLockBasedBackend;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockBasedTransaction;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockResult;

import java.util.HashMap;
import java.util.Map;

public class LockBasedBackendMock<K> extends AbstractLockBasedBackend<K> {

    private final Map<K, byte[]> stateMap = new HashMap<>();

    public LockBasedBackendMock(TimeMeter timeMeter) {
        super(timeMeter);
    }

    @Override
    protected LockBasedTransaction allocateTransaction(K key) {
        byte[] backup = stateMap.get(key);

        return new LockBasedTransaction() {
            @Override
            public void begin() {
                // do nothing
            }

            @Override
            public void update(byte[] data) {
                if (backup == null) {
                    throw new IllegalStateException();
                }
                stateMap.put(key, data);
            }

            @Override
            public void create(byte[] data) {
                if (backup != null) {
                    throw new IllegalStateException();
                }
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

            @Override
            public LockResult lock() {
                return backup == null? LockResult.DATA_NOT_EXISTS_AND_LOCKED : LockResult.DATA_EXISTS_AND_LOCKED;
            }

            @Override
            public void unlock() {
                // do nothing
            }

            @Override
            public byte[] getData() {
                if (backup == null) {
                    throw new IllegalStateException();
                }
                return backup;
            }

        };
    }

    @Override
    protected void releaseTransaction(LockBasedTransaction transaction) {
        // do nothing
    }

}
