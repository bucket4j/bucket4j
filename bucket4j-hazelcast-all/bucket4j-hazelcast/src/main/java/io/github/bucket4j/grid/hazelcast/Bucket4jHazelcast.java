/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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

import java.util.Objects;

import com.hazelcast.map.IMap;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

/**
 * Entry point for Hazelcast integration
 */
public class Bucket4jHazelcast {

    /**
     * Returns the builder for {@link HazelcastProxyManager}
     *
     * @param map
     *
     * @return new instance of {@link HazelcastProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> HazelcastProxyManagerBuilder<K> entryProcessorBasedBuilder(IMap<K, byte[]> map) {
        return new HazelcastProxyManagerBuilder<>(map);
    }

    /**
     * Returns the builder for {@link HazelcastLockBasedProxyManager}
     *
     * @param map
     *
     * @return new instance of {@link HazelcastLockBasedProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> HazelcastLockBasedProxyManagerBuilder<K> lockBasedBuilder(IMap<K, byte[]> map) {
        return new HazelcastLockBasedProxyManagerBuilder<>(map);
    }

    /**
     * Returns the builder for {@link HazelcastCompareAndSwapBasedProxyManager}
     *
     * @param map
     *
     * @return new instance of {@link HazelcastCompareAndSwapBasedProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> HazelcastCompareAndSwapBasedProxyManagerBuilder<K> casBasedBuilder(IMap<K, byte[]> map) {
        return new HazelcastCompareAndSwapBasedProxyManagerBuilder<>(map);
    }

    public static class HazelcastProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, HazelcastProxyManager<K>, HazelcastProxyManagerBuilder<K>> {

        final IMap<K, byte[]> map;
        String offloadableExecutorName;

        public HazelcastProxyManagerBuilder(IMap<K, byte[]> map) {
            this.map = Objects.requireNonNull(map);
        }

        public HazelcastProxyManagerBuilder<K> offloadableExecutorName(String offloadableExecutorName) {
            this.offloadableExecutorName = Objects.requireNonNull(offloadableExecutorName);
            return this;
        }

        @Override
        public HazelcastProxyManager<K> build() {
            return new HazelcastProxyManager<>(this);
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }
    }

    public static class HazelcastCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, HazelcastCompareAndSwapBasedProxyManager<K>, HazelcastCompareAndSwapBasedProxyManagerBuilder<K>> {

        final IMap<K, byte[]> map;

        public HazelcastCompareAndSwapBasedProxyManagerBuilder(IMap<K, byte[]> map) {
            this.map = Objects.requireNonNull(map);
        }

        @Override
        public HazelcastCompareAndSwapBasedProxyManager<K> build() {
            return new HazelcastCompareAndSwapBasedProxyManager<>(this);
        }
    }

    public static class HazelcastLockBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, HazelcastLockBasedProxyManager<K>, HazelcastLockBasedProxyManagerBuilder<K>> {

        final IMap<K, byte[]> map;

        public HazelcastLockBasedProxyManagerBuilder(IMap<K, byte[]> map) {
            this.map = Objects.requireNonNull(map);
        }

        @Override
        public HazelcastLockBasedProxyManager<K> build() {
            return new HazelcastLockBasedProxyManager<>(this);
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }
    }

}
