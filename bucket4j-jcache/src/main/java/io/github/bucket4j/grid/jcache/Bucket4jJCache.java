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
package io.github.bucket4j.grid.jcache;

import java.util.Objects;

import javax.cache.Cache;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

/**
 * Entry point for JCache integration
 */
public class Bucket4jJCache {

    /**
     * Returns the builder for {@link JCacheProxyManager}
     *
     * @param cache
     *
     * @return new instance of {@link JCacheProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> JCacheProxyManagerBuilder<K> entryProcessorBasedBuilder(Cache<K, byte[]> cache) {
        return new JCacheProxyManagerBuilder<>(cache);
    }

    public static class JCacheProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, JCacheProxyManager<K>, JCacheProxyManagerBuilder<K>> {

        final Cache<K, byte[]> cache;

        public JCacheProxyManagerBuilder(Cache<K, byte[]> cache) {
            this.cache = Objects.requireNonNull(cache);
        }

        @Override
        public JCacheProxyManager<K> build() {
            return new JCacheProxyManager<>(this);
        }

    }

}
