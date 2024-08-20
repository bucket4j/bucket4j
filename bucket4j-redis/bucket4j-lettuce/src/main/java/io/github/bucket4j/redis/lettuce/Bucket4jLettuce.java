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
package io.github.bucket4j.redis.lettuce;

import java.util.Objects;

import io.github.bucket4j.distributed.proxy.AbstractAsyncProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceAsyncProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;

/**
 * Entry point for Lettuce integration
 */
public class Bucket4jLettuce {

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param redisAsyncCommands
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static <K> LettuceBasedProxyManagerBuilder<K> casBasedBuilder(RedisAsyncCommands<K, byte[]> redisAsyncCommands) {
        Objects.requireNonNull(redisAsyncCommands);
        RedisApi<K> redisApi = new RedisApi<>() {
            @Override
            public <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, K[] keys, byte[][] params) {
                return redisAsyncCommands.eval(script, scriptOutputType, keys, params);
            }
            @Override
            public RedisFuture<byte[]> get(K key) {
                return redisAsyncCommands.get(key);
            }
            @Override
            public RedisFuture<?> delete(K key) {
                return redisAsyncCommands.del(key);
            }
        };
        return new LettuceBasedProxyManagerBuilder<>(redisApi);
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param statefulRedisConnection
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static <K> LettuceBasedProxyManagerBuilder<K> casBasedBuilder(StatefulRedisConnection<K, byte[]> statefulRedisConnection) {
        return casBasedBuilder(statefulRedisConnection.async());
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param redisClient
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static LettuceBasedProxyManagerBuilder<byte[]> casBasedBuilder(RedisClient redisClient) {
        return casBasedBuilder(redisClient.connect(ByteArrayCodec.INSTANCE));
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param redisClient
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static LettuceBasedProxyManagerBuilder<byte[]> casBasedBuilder(RedisClusterClient redisClient) {
        return casBasedBuilder(redisClient.connect(ByteArrayCodec.INSTANCE));
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param connection
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static <K> LettuceBasedProxyManagerBuilder<K> casBasedBuilder(StatefulRedisClusterConnection<K, byte[]> connection) {
        return casBasedBuilder(connection.async());
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param redisAsyncCommands
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static <K> LettuceBasedProxyManagerBuilder<K> casBasedBuilder(RedisAdvancedClusterAsyncCommands<K, byte[]> redisAsyncCommands) {
        Objects.requireNonNull(redisAsyncCommands);
        RedisApi<K> redisApi = new RedisApi<>() {
            @Override
            public <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, K[] keys, byte[][] params) {
                return redisAsyncCommands.eval(script, scriptOutputType, keys, params);
            }
            @Override
            public RedisFuture<byte[]> get(K key) {
                return redisAsyncCommands.get(key);
            }
            @Override
            public RedisFuture<?> delete(K key) {
                return redisAsyncCommands.del(key);
            }
        };
        return new LettuceBasedProxyManagerBuilder<>(redisApi);
    }

    /**
     * Returns the builder for {@link LettuceAsyncProxyManager}
     *
     * @param redisAsyncCommands
     *
     * @return new instance of {@link LettuceAsyncProxyManagerBuilder}
     */
    public static <K> LettuceAsyncProxyManagerBuilder<K> asyncCasBasedBuilder(RedisAsyncCommands<K, byte[]> redisAsyncCommands) {
        Objects.requireNonNull(redisAsyncCommands);
        RedisApi<K> redisApi = new RedisApi<>() {
            @Override
            public <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, K[] keys, byte[][] params) {
                return redisAsyncCommands.eval(script, scriptOutputType, keys, params);
            }
            @Override
            public RedisFuture<byte[]> get(K key) {
                return redisAsyncCommands.get(key);
            }
            @Override
            public RedisFuture<?> delete(K key) {
                return redisAsyncCommands.del(key);
            }
        };
        return new LettuceAsyncProxyManagerBuilder<>(redisApi);
    }

    /**
     * Returns the builder for {@link LettuceAsyncProxyManager}
     *
     * @param statefulRedisConnection
     *
     * @return new instance of {@link LettuceAsyncProxyManagerBuilder}
     */
    public static <K> LettuceAsyncProxyManagerBuilder<K> asyncCasBasedBuilder(StatefulRedisConnection<K, byte[]> statefulRedisConnection) {
        return asyncCasBasedBuilder(statefulRedisConnection.async());
    }

    /**
     * Returns the builder for {@link LettuceAsyncProxyManager}
     *
     * @param redisClient
     *
     * @return new instance of {@link LettuceAsyncProxyManagerBuilder}
     */
    public static LettuceAsyncProxyManagerBuilder<byte[]> asyncCasBasedBuilder(RedisClient redisClient) {
        return asyncCasBasedBuilder(redisClient.connect(ByteArrayCodec.INSTANCE));
    }

    /**
     * Returns the builder for {@link LettuceAsyncProxyManager}
     *
     * @param redisClient
     *
     * @return new instance of {@link LettuceAsyncProxyManagerBuilder}
     */
    public static LettuceAsyncProxyManagerBuilder<byte[]> asyncCasBasedBuilder(RedisClusterClient redisClient) {
        return asyncCasBasedBuilder(redisClient.connect(ByteArrayCodec.INSTANCE));
    }

    /**
     * Returns the builder for {@link LettuceAsyncProxyManager}
     *
     * @param connection
     *
     * @return new instance of {@link LettuceAsyncProxyManagerBuilder}
     */
    public static <K> LettuceAsyncProxyManagerBuilder<K> asyncCasBasedBuilder(StatefulRedisClusterConnection<K, byte[]> connection) {
        return asyncCasBasedBuilder(connection.async());
    }

    /**
     * Returns the builder for {@link LettuceAsyncProxyManager}
     *
     * @param redisAsyncCommands
     *
     * @return new instance of {@link LettuceAsyncProxyManagerBuilder}
     */
    public static <K> LettuceAsyncProxyManagerBuilder<K> asyncCasBasedBuilder(RedisAdvancedClusterAsyncCommands<K, byte[]> redisAsyncCommands) {
        Objects.requireNonNull(redisAsyncCommands);
        RedisApi<K> redisApi = new RedisApi<>() {
            @Override
            public <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, K[] keys, byte[][] params) {
                return redisAsyncCommands.eval(script, scriptOutputType, keys, params);
            }
            @Override
            public RedisFuture<byte[]> get(K key) {
                return redisAsyncCommands.get(key);
            }
            @Override
            public RedisFuture<?> delete(K key) {
                return redisAsyncCommands.del(key);
            }
        };
        return new LettuceAsyncProxyManagerBuilder<>(redisApi);
    }

    public static class LettuceBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, LettuceBasedProxyManager<K>, LettuceBasedProxyManagerBuilder<K>> {

        private final RedisApi<K> redisApi;

        public LettuceBasedProxyManagerBuilder(RedisApi<K> redisApi) {
            this.redisApi = redisApi;
        }

        public RedisApi<K> getRedisApi() {
            return redisApi;
        }

        @Override
        public LettuceBasedProxyManager<K> build() {
            return new LettuceBasedProxyManager<>(this);
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

    }

    public static class LettuceAsyncProxyManagerBuilder<K> extends AbstractAsyncProxyManagerBuilder<K, LettuceAsyncProxyManager<K>, LettuceAsyncProxyManagerBuilder<K>> {

        private final RedisApi<K> redisApi;

        public LettuceAsyncProxyManagerBuilder(RedisApi<K> redisApi) {
            this.redisApi = redisApi;
        }

        public RedisApi<K> getRedisApi() {
            return redisApi;
        }

        @Override
        public LettuceAsyncProxyManager<K> build() {
            return new LettuceAsyncProxyManager<>(this);
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

    }

}
