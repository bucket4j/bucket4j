/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.core.IMap;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.LockBasedTransaction;

import java.util.Objects;

public class HazelcastLockBasedProxyManager<K> extends AbstractLockBasedProxyManager<K> {

    private final IMap<K, byte[]> map;

    public HazelcastLockBasedProxyManager(IMap<K, byte[]> map) {
        this(map, ClientSideConfig.getDefault());
    }

    public HazelcastLockBasedProxyManager(IMap<K, byte[]> map, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.map = Objects.requireNonNull(map);
    }

    @Override
    public void removeProxy(K key) {
        map.remove(key);
    }

    @Override
    public boolean isAsyncModeSupported() {
        // Because Hazelcast IMap does not provide "lockAsync" API.
        return false;
    }

    @Override
    protected LockBasedTransaction allocateTransaction(K key) {
        return new LockBasedTransaction() {

            @Override
            public void begin() {
                // do nothing
            }

            @Override
            public void rollback() {
                // do nothing
            }

            @Override
            public void commit() {
                // do nothing
            }

            @Override
            public byte[] lockAndGet() {
                map.lock(key);
                return map.get(key);
            }

            @Override
            public void unlock() {
                map.unlock(key);
            }

            @Override
            public void create(byte[] data) {
                map.put(key, data);
            }

            @Override
            public void update(byte[] data) {
                map.put(key, data);
            }

            @Override
            public void release() {
                // do nothing
            }
        };
    }

}
