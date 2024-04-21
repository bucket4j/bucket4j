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
package io.github.bucket4j.grid.ignite.thick;

import java.util.Objects;

import org.apache.ignite.IgniteCache;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

public class Bucket4jIgniteThick {

    public static final Bucket4jIgniteThick INSTANCE = new Bucket4jIgniteThick();

    /**
     * Returns the builder for {@link IgniteProxyManager}
     *
     * @param cache
     *
     * @return new instance of {@link IgniteProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public <K> IgniteProxyManagerBuilder<K> entryProcessorBasedBuilder(IgniteCache<K, byte[]> cache) {
        return new IgniteProxyManagerBuilder<>(cache);
    }

    public static class IgniteProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, IgniteProxyManager<K>, IgniteProxyManagerBuilder<K>> {

        final IgniteCache<K, byte[]> cache;

        public IgniteProxyManagerBuilder(IgniteCache<K, byte[]> cache) {
            this.cache = Objects.requireNonNull(cache);
        }

        @Override
        public IgniteProxyManager<K> build() {
            return new IgniteProxyManager<>(this);
        }

    }



}
