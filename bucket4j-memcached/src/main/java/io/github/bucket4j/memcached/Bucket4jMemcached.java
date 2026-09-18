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

import java.time.Duration;
import java.util.Objects;

import com.spotify.folsom.MemcacheClient;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.memcached.cas.MemcachedCompareAndSwapBasedProxyManager;
import io.github.bucket4j.memcached.lock.MemcachedLockBasedProxyManager;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

/**
 * Entry point for Memcached integration that uses the <a href="https://github.com/spotify/folsom">folsom</a> client.
 *
 * <p>
 * Memcached does not provide server-side scripting/RPC capabilities, therefore two complementary strategies are provided:
 * <ul>
 *     <li>{@link #casBasedBuilder(MemcacheClient)} - uses memcached's native {@code gets}/{@code cas}/{@code add} commands
 *     to implement optimistic concurrency control. Works against any memcached-protocol-compatible server and does not require
 *     server-side locking support. Supports both the synchronous and the asynchronous {@code Bucket} API, since folsom's own
 *     API is natively based on {@link java.util.concurrent.CompletionStage}.</li>
 *     <li>{@link #lockBasedBuilder(MemcacheClient)} - emulates a distributed mutex on top of the {@code add} command
 *     (which succeeds only if the key is currently absent) and uses it to guard a plain read-modify-write cycle. Only
 *     the synchronous {@code Bucket} API is supported, because lock acquisition inherently requires a blocking retry loop.</li>
 * </ul>
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

    /**
     * Returns the builder for {@link MemcachedLockBasedProxyManager}.
     *
     * @param client folsom client that holds the connection(s) to the memcached cluster.
     *
     * @return new instance of {@link MemcachedLockBasedProxyManagerBuilder}
     */
    public static MemcachedLockBasedProxyManagerBuilder<String> lockBasedBuilder(MemcacheClient<byte[]> client) {
        return new MemcachedLockBasedProxyManagerBuilder<>(client, STRING);
    }

    /**
     * Returns the builder for {@link MemcachedLockBasedProxyManager}.
     *
     * @param client folsom client that holds the connection(s) to the memcached cluster.
     * @param keyMapper object responsible for converting primary keys to memcached keys.
     * @param <K> type of primary key
     *
     * @return new instance of {@link MemcachedLockBasedProxyManagerBuilder}
     */
    public static <K> MemcachedLockBasedProxyManagerBuilder<K> lockBasedBuilder(MemcacheClient<byte[]> client, Mapper<K> keyMapper) {
        return new MemcachedLockBasedProxyManagerBuilder<>(client, keyMapper);
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

    public static class MemcachedLockBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, MemcachedLockBasedProxyManager<K>, MemcachedLockBasedProxyManagerBuilder<K>> {

        private static final int DEFAULT_LOCK_EXPIRATION_SECONDS = 30;
        private static final long DEFAULT_LOCK_POLL_PERIOD_MILLIS = 50;

        private final MemcacheClient<byte[]> client;
        private Mapper<K> keyMapper;
        private int lockExpirationSeconds = DEFAULT_LOCK_EXPIRATION_SECONDS;
        private long lockPollPeriodMillis = DEFAULT_LOCK_POLL_PERIOD_MILLIS;

        private MemcachedLockBasedProxyManagerBuilder(MemcacheClient<byte[]> client, Mapper<K> keyMapper) {
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
        public <K2> MemcachedLockBasedProxyManagerBuilder<K2> keyMapper(Mapper<K2> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (MemcachedLockBasedProxyManagerBuilder<K2>) this;
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
        }

        /**
         * Specifies for how long the mutex that guards a bucket is allowed to stay held in memcached before it is
         * automatically released. Protects against a permanently stuck lock in case a client crashes while holding it.
         *
         * <p> By default, the lock expiration is 30 seconds.
         *
         * @param lockExpiration duration after which a held lock is automatically released by memcached.
         *
         * @return this builder instance
         */
        public MemcachedLockBasedProxyManagerBuilder<K> lockExpiration(Duration lockExpiration) {
            Objects.requireNonNull(lockExpiration);
            this.lockExpirationSeconds = (int) Math.max(1, lockExpiration.toSeconds());
            return this;
        }

        public int getLockExpirationSeconds() {
            return lockExpirationSeconds;
        }

        /**
         * Specifies how often a client that failed to acquire the mutex retries acquiring it.
         *
         * <p> By default, the poll period is 50 milliseconds.
         *
         * @param lockPollPeriod delay between successive attempts to acquire the mutex.
         *
         * @return this builder instance
         */
        public MemcachedLockBasedProxyManagerBuilder<K> lockPollPeriod(Duration lockPollPeriod) {
            Objects.requireNonNull(lockPollPeriod);
            this.lockPollPeriodMillis = Math.max(1, lockPollPeriod.toMillis());
            return this;
        }

        public long getLockPollPeriodMillis() {
            return lockPollPeriodMillis;
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

        @Override
        public MemcachedLockBasedProxyManager<K> build() {
            return new MemcachedLockBasedProxyManager<>(this);
        }
    }

}
