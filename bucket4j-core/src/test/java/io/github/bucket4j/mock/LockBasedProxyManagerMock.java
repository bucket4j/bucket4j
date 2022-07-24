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
import io.github.bucket4j.distributed.remote.RemoteBucketState;

import java.util.HashMap;
import java.util.Map;

public class LockBasedProxyManagerMock<K> extends AbstractLockBasedProxyManager<K> {

    private final Map<K, byte[]> stateMap = new HashMap<>();

    public LockBasedProxyManagerMock(ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
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
            public void update(byte[] data, RemoteBucketState newState) {
                if (backup == null) {
                    throw new IllegalStateException();
                }
                stateMap.put(key, data);
            }

            @Override
            public void release() {
                // do nothing
            }

            @Override
            public void create(byte[] data, RemoteBucketState newState) {
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
            public byte[] lockAndGet() {
                return backup;
            }

            @Override
            public void unlock() {
                // do nothing
            }

        };
    }

    @Override
    public void removeProxy(K key) {
        stateMap.remove(key);
    }

}
