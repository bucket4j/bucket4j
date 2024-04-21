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
package io.github.bucket4j.grid.coherence;

import java.util.Objects;

import com.tangosol.net.NamedCache;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

/**
 * Entry point for Coherence integration
 */
public class Bucket4jCoherence {

    /**
     * Returns the builder for {@link CoherenceProxyManager}
     *
     * @param cache
     *
     * @return new instance of {@link CoherenceProxyManagerBuilder}
     * @param <K> type ok key
     */
    public static <K> CoherenceProxyManagerBuilder<K> entryProcessorBasedBuilder(NamedCache<K, byte[]> cache) {
        return new CoherenceProxyManagerBuilder<>(cache);
    }

    public static class CoherenceProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, CoherenceProxyManager<K>, CoherenceProxyManagerBuilder<K>> {

        final NamedCache<K, byte[]> cache;

        public CoherenceProxyManagerBuilder(NamedCache<K, byte[]> cache) {
            this.cache = Objects.requireNonNull(cache);
        }

        @Override
        public CoherenceProxyManager<K> build() {
            return new CoherenceProxyManager<>(this);
        }

    }

}
