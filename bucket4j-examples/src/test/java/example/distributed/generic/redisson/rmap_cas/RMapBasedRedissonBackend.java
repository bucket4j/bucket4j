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

package example.distributed.generic.redisson.rmap_cas;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedBackend;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapBasedTransaction;
import org.redisson.api.RMap;

import java.sql.*;
import java.util.Optional;

public class RMapBasedRedissonBackend extends AbstractCompareAndSwapBasedBackend<Long> {

    private final RMap<Long, byte[]> buckets;

    public RMapBasedRedissonBackend(RMap<Long, byte[]> buckets, ClientSideConfig clientSideConfig) throws SQLException {
        super(clientSideConfig);
        this.buckets = buckets;
    }

    @Override
    protected CompareAndSwapBasedTransaction allocateTransaction(Long key) {
        return new RedissonCompareAndSwapTransaction(key);
    }

    @Override
    protected void releaseTransaction(CompareAndSwapBasedTransaction transaction) {
        // do nothing
    }



    private class RedissonCompareAndSwapTransaction implements CompareAndSwapBasedTransaction {

        private final long key;

        private RedissonCompareAndSwapTransaction(long key) {
            this.key = key;
        }


        @Override
        public Optional<byte[]> get() {
            byte[] persistedState = buckets.get(key);
            return Optional.ofNullable(persistedState);
        }

        @Override
        public boolean compareAndSwap(byte[] originalData, byte[] newData) {
            if (originalData == null) {
                // Redisson prohibits the usage null as values, so "replace" must not be used in such cases
                return buckets.putIfAbsent(key, newData) == null;
            }
            return buckets.replace(key, originalData, newData);
        }

    }

}
