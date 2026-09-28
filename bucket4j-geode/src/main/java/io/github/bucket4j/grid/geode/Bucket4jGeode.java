/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.geode;

import java.util.Objects;

import org.apache.geode.cache.Region;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;

/**
 * Entry point for Apache Geode (GemFire) integration
 */
public class Bucket4jGeode {

    /**
     * Returns the builder for {@link GeodeProxyManager}
     *
     * @param region the region that will be used to store bucket state, must contain {@code byte[]} values
     *
     * @return new instance of {@link GeodeProxyManagerBuilder}
     * @param <K> type of key
     */
    public static <K> GeodeProxyManagerBuilder<K> compareAndSwapBasedBuilder(Region<K, byte[]> region) {
        return new GeodeProxyManagerBuilder<>(region);
    }

    public static class GeodeProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, GeodeProxyManager<K>, GeodeProxyManagerBuilder<K>> {

        final Region<K, byte[]> region;

        public GeodeProxyManagerBuilder(Region<K, byte[]> region) {
            this.region = Objects.requireNonNull(region);
        }

        @Override
        public GeodeProxyManager<K> build() {
            return new GeodeProxyManager<>(this);
        }

    }

    /**
     * Returns the builder for {@link GeodeFunctionProxyManager}, which colocates the whole compare-and-swap
     * retry loop with the data via a Geode {@code Function}, instead of retrying from the client the way
     * the proxy manager returned by {@link #compareAndSwapBasedBuilder(Region)} does. Requires the Bucket4j
     * jar to be present on the classpath of every Geode server that can own a bucket key.
     *
     * @param region the region that will be used to store bucket state, must contain {@code byte[]} values
     *
     * @return new instance of {@link GeodeFunctionProxyManagerBuilder}
     * @param <K> type of key
     */
    public static <K> GeodeFunctionProxyManagerBuilder<K> functionBasedBuilder(Region<K, byte[]> region) {
        return new GeodeFunctionProxyManagerBuilder<>(region);
    }

    public static class GeodeFunctionProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, GeodeFunctionProxyManager<K>, GeodeFunctionProxyManagerBuilder<K>> {

        final Region<K, byte[]> region;

        public GeodeFunctionProxyManagerBuilder(Region<K, byte[]> region) {
            this.region = Objects.requireNonNull(region);
        }

        @Override
        public GeodeFunctionProxyManager<K> build() {
            return new GeodeFunctionProxyManager<>(this);
        }

    }

}
