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

import org.infinispan.functional.FunctionalMap;


import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

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

}
