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
package io.github.bucket4j.grid.infinispan;

import java.util.Objects;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.functional.FunctionalMap;


import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.grid.infinispan.hotrod.HotrodInfinispanProxyManager;

/**
 * Entry point for Infinispan integration
 */
public class Bucket4jInfinispan {

    /**
     * Returns the builder for {@link InfinispanProxyManager}
     *
     * @param readWriteMap
     *
     * @return new instance of {@link InfinispanProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> InfinispanProxyManagerBuilder<K> entryProcessorBasedBuilder(FunctionalMap.ReadWriteMap<K, byte[]> readWriteMap) {
        return new InfinispanProxyManagerBuilder<>(readWriteMap);
    }

    /**
     * Returns the builder for {@link HotrodInfinispanProxyManager}
     *
     * @param remoteCache
     *
     * @return new instance of {@link HotrodInfinispanProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> HotrodInfinispanProxyManagerBuilder<K> hotrodClientBasedBuilder(RemoteCache<K, byte[]> remoteCache) {
        return new HotrodInfinispanProxyManagerBuilder<>(remoteCache);
    }

    public static class InfinispanProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, InfinispanProxyManager<K>, InfinispanProxyManagerBuilder<K>> {

        final FunctionalMap.ReadWriteMap<K, byte[]> readWriteMap;

        public InfinispanProxyManagerBuilder(FunctionalMap.ReadWriteMap<K, byte[]> readWriteMap) {
            this.readWriteMap = Objects.requireNonNull(readWriteMap);
        }

        @Override
        public InfinispanProxyManager<K> build() {
            return new InfinispanProxyManager<>(this);
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

    }

    public static class HotrodInfinispanProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, HotrodInfinispanProxyManager<K>, HotrodInfinispanProxyManagerBuilder<K>> {

        public final RemoteCache<K, byte[]> remoteCache;

        public HotrodInfinispanProxyManagerBuilder(RemoteCache<K, byte[]> remoteCache) {
            this.remoteCache = Objects.requireNonNull(remoteCache);
        }

        @Override
        public HotrodInfinispanProxyManager<K> build() {
            return new HotrodInfinispanProxyManager<>(this);
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

    }

}
