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
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.LockBasedTransaction;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractSelectForUpdateBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockAndGetResult;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.SelectForUpdateBasedTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SelectForUpdateBasedProxyManagerMock<K> extends AbstractSelectForUpdateBasedProxyManager<K> {

    private final Map<K, byte[]> stateMap = new HashMap<>();

    public SelectForUpdateBasedProxyManagerMock(ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
    }

    @Override
    protected SelectForUpdateBasedTransaction allocateTransaction(K key, Optional<Long> requestTimeoutNanos) {
        boolean existBeforeTransaction = stateMap.containsKey(key);
        byte[] backup = stateMap.get(key);

        return new SelectForUpdateBasedTransaction() {

            @Override
            public void begin(Optional<Long> requestTimeoutNanos) {
                // do nothing
            }

            @Override
            public void update(byte[] data, RemoteBucketState newState, Optional<Long> requestTimeoutNanos) {
                if (!existBeforeTransaction) {
                    throw new IllegalStateException();
                }
                stateMap.put(key, data);
            }

            @Override
            public void release() {
                // do nothing
            }

            @Override
            public void rollback(Optional<Long> requestTimeoutNanos) {
                stateMap.put(key, backup);
            }

            @Override
            public void commit(Optional<Long> requestTimeoutNanos) {
                // do nothing
            }

            @Override
            public LockAndGetResult tryLockAndGet(Optional<Long> requestTimeoutNanos) {
                if (!existBeforeTransaction) {
                    return LockAndGetResult.notLocked();
                }
                return LockAndGetResult.locked(backup);
            }

            @Override
            public boolean tryInsertEmptyData(Optional<Long> requestTimeoutNanos) {
                if (existBeforeTransaction) {
                    throw new IllegalStateException();
                }
                stateMap.put(key, null);
                return true;
            }

        };
    }

    @Override
    public void removeProxy(K key) {
        stateMap.remove(key);
    }

}
