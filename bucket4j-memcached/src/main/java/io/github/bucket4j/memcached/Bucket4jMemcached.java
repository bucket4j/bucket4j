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
package io.github.bucket4j.memcached;

import java.util.Objects;

import com.spotify.folsom.MemcacheClient;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.memcached.cas.MemcachedCompareAndSwapBasedProxyManager;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

/**
 * Entry point for Memcached integration that uses the <a href="https://github.com/spotify/folsom">folsom</a> client.
 *
 * <p>
 * Memcached does not provide server-side scripting/RPC capabilities, therefore the bucket state transition is computed
 * on the client and persisted using memcached's native {@code gets}/{@code cas}/{@code add} commands to implement
 * optimistic concurrency control. Works against any memcached-protocol-compatible server and does not require
 * server-side locking support. Supports both the synchronous and the asynchronous {@code Bucket} API, since folsom's
 * own API is natively based on {@link java.util.concurrent.CompletionStage}.
 */
public class Bucket4jMemcached {

    /**
     * Returns the builder for {@link MemcachedCompareAndSwapBasedProxyManager}.
     *
     * @param client folsom client that holds the connection(s) to the memcached cluster.
     *
     * @return new instance of {@link MemcachedCompareAndSwapBasedProxyManagerBuilder}
     */
    public static MemcachedCompareAndSwapBasedProxyManagerBuilder<String> casBasedBuilder(MemcacheClient<byte[]> client) {
        return new MemcachedCompareAndSwapBasedProxyManagerBuilder<>(client, STRING);
    }

    /**
     * Returns the builder for {@link MemcachedCompareAndSwapBasedProxyManager}.
     *
     * @param client folsom client that holds the connection(s) to the memcached cluster.
     * @param keyMapper object responsible for converting primary keys to memcached keys.
     * @param <K> type of primary key
     *
     * @return new instance of {@link MemcachedCompareAndSwapBasedProxyManagerBuilder}
     */
    public static <K> MemcachedCompareAndSwapBasedProxyManagerBuilder<K> casBasedBuilder(MemcacheClient<byte[]> client, Mapper<K> keyMapper) {
        return new MemcachedCompareAndSwapBasedProxyManagerBuilder<>(client, keyMapper);
    }

    public static class MemcachedCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, MemcachedCompareAndSwapBasedProxyManager<K>, MemcachedCompareAndSwapBasedProxyManagerBuilder<K>> {

        private final MemcacheClient<byte[]> client;
        private Mapper<K> keyMapper;

        private MemcachedCompareAndSwapBasedProxyManagerBuilder(MemcacheClient<byte[]> client, Mapper<K> keyMapper) {
            this.client = Objects.requireNonNull(client);
            this.keyMapper = Objects.requireNonNull(keyMapper);
        }

        public MemcacheClient<byte[]> getClient() {
            return client;
        }

        /**
         * Specifies the type of key.
         *
         * @param keyMapper object responsible for converting primary keys to memcached keys.
         *
         * @return this builder instance
         */
        public <K2> MemcachedCompareAndSwapBasedProxyManagerBuilder<K2> keyMapper(Mapper<K2> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (MemcachedCompareAndSwapBasedProxyManagerBuilder<K2>) this;
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

        @Override
        public MemcachedCompareAndSwapBasedProxyManager<K> build() {
            return new MemcachedCompareAndSwapBasedProxyManager<>(this);
        }
    }

}
