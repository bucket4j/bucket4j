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
package io.github.bucket4j.caffeine;

import java.util.Objects;

import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

/**
 * Entry point for Caffeine integration
 */
public class Bucket4jCaffeine {

    /**
     * Returns the builder for {@link CaffeineProxyManager}
     *
     * @param cacheBuilder
     *
     * @return new instance of {@link CaffeineProxyManagerBuilder}
     *
     * @param <K> type ok key
     */
    public static <K> CaffeineProxyManagerBuilder<K> builderFor(Caffeine<?, ?> cacheBuilder) {
        return new CaffeineProxyManagerBuilder<>((Caffeine) cacheBuilder);
    }

    public static class CaffeineProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, CaffeineProxyManager<K>, CaffeineProxyManagerBuilder<K>> {

        final Caffeine<K, RemoteBucketState> cacheBuilder;

        public CaffeineProxyManagerBuilder(Caffeine<K, RemoteBucketState> cacheBuilder) {
            this.cacheBuilder = Objects.requireNonNull(cacheBuilder);
        }

        @Override
        public CaffeineProxyManager<K> build() {
            return new CaffeineProxyManager<>(this);
        }

    }

}
