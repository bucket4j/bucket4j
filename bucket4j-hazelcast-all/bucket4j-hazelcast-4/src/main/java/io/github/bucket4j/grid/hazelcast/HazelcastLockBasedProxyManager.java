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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.hazelcast.map.IMap;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.LockBasedTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

public class HazelcastLockBasedProxyManager<K> extends AbstractLockBasedProxyManager<K> {

    private final IMap<K, byte[]> map;

    public HazelcastLockBasedProxyManager(Bucket4jHazelcast.HazelcastLockBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.map = builder.map;
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
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    @Override
    protected LockBasedTransaction allocateTransaction(K key, Optional<Long> requestTimeout) {
        return new LockBasedTransaction() {

            @Override
            public void begin(Optional<Long> requestTimeout) {
                // do nothing
            }

            @Override
            public void rollback() {
                // do nothing
            }

            @Override
            public void commit(Optional<Long> requestTimeout) {
                // do nothing
            }

            @Override
            public byte[] lockAndGet(Optional<Long> requestTimeout) {
                map.lock(key);
                return map.get(key);
            }

            @Override
            public void unlock() {
                map.unlock(key);
            }

            @Override
            public void create(byte[] data, RemoteBucketState newState, Optional<Long> requestTimeout) {
                save(data, newState);
            }

            @Override
            public void update(byte[] data, RemoteBucketState newState, Optional<Long> requestTimeout) {
                save(data, newState);
            }

            private void save(byte[] data, RemoteBucketState newState) {
                ExpirationAfterWriteStrategy expiration = getClientSideConfig().getExpirationAfterWriteStrategy().orElse(null);
                if (expiration == null) {
                    map.put(key, data);
                } else {
                    long currentTimeNanos = getClientSideConfig().getClientSideClock().orElse(TimeMeter.SYSTEM_MILLISECONDS).currentTimeNanos();
                    long ttlMillis = expiration.calculateTimeToLiveMillis(newState, currentTimeNanos);
                    if (ttlMillis > 0) {
                        map.put(key, data, ttlMillis, TimeUnit.MILLISECONDS);
                    } else {
                        map.put(key, data);
                    }
                }
            }

            @Override
            public void release() {
                // do nothing
            }
        };
    }

}
