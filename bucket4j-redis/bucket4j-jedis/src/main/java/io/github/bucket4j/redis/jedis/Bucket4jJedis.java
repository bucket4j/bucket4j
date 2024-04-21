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
package io.github.bucket4j.redis.jedis;

import java.util.Objects;


import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.util.Pool;

/**
 * Entry point for Jedis integration
 */
public class Bucket4jJedis {

    /**
     * Returns the builder for {@link JedisBasedProxyManager}
     *
     * @param jedisPool
     *
     * @return new instance of {@link JedisBasedProxyManagerBuilder}
     */
    public static JedisBasedProxyManagerBuilder<byte[]> casBasedBuilder(Pool<Jedis> jedisPool) {
        Objects.requireNonNull(jedisPool);
        RedisApi redisApi = new RedisApi() {
            @Override
            public Object eval(byte[] script, int keyCount, byte[]... params) {
                try (Jedis jedis = jedisPool.getResource()) {
                    return jedis.eval(script, 1, params);
                }
            }
            @Override
            public byte[] get(byte[] key) {
                try (Jedis jedis = jedisPool.getResource()) {
                    return jedis.get(key);
                }
            }
            @Override
            public void delete(byte[] key) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.del(key);
                }
            }
        };
        return new JedisBasedProxyManagerBuilder<>(Mapper.BYTES, redisApi);
    }

    /**
     * Returns the builder for {@link JedisBasedProxyManager}
     *
     * @param unifiedJedis
     *
     * @return new instance of {@link JedisBasedProxyManagerBuilder}
     */
    public static JedisBasedProxyManagerBuilder<byte[]> casBasedBuilder(UnifiedJedis unifiedJedis) {
        Objects.requireNonNull(unifiedJedis);
        RedisApi redisApi = new RedisApi() {
            @Override
            public Object eval(byte[] script, int keyCount, byte[]... params) {
                return unifiedJedis.eval(script, keyCount, params);
            }

            @Override
            public byte[] get(byte[] key) {
                return unifiedJedis.get(key);
            }

            @Override
            public void delete(byte[] key) {
                unifiedJedis.del(key);
            }
        };
        return new JedisBasedProxyManagerBuilder<>(Mapper.BYTES, redisApi);

    }

    /**
     * Returns the builder for {@link JedisBasedProxyManager}
     *
     * @param jedisCluster
     *
     * @return new instance of {@link JedisBasedProxyManagerBuilder}
     */
    public static JedisBasedProxyManagerBuilder<byte[]> casBasedBuilder(JedisCluster jedisCluster) {
        Objects.requireNonNull(jedisCluster);
        RedisApi redisApi = new RedisApi() {
            @Override
            public Object eval(byte[] script, int keyCount, byte[]... params) {
                return jedisCluster.eval(script, keyCount, params);
            }
            @Override
            public byte[] get(byte[] key) {
                return jedisCluster.get(key);
            }
            @Override
            public void delete(byte[] key) {
                jedisCluster.del(key);
            }
        };
        return new JedisBasedProxyManagerBuilder<>(Mapper.BYTES, redisApi);
    }

    public static class JedisBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, JedisBasedProxyManager<K>, JedisBasedProxyManagerBuilder<K>> {

        final RedisApi redisApi;
        Mapper<K> keyMapper;

        public JedisBasedProxyManagerBuilder(Mapper<K> keyMapper, RedisApi redisApi) {
            this.redisApi = redisApi;
            this.keyMapper = Objects.requireNonNull(keyMapper);
        }

        @Override
        public JedisBasedProxyManager<K> build() {
            return new JedisBasedProxyManager<>(this);
        }

        /**
         * Specifies the type of key.
         *
         * @param keyMapper object responsible for converting primary keys to byte arrays.
         *
         * @return this builder instance
         */
        public <K2> JedisBasedProxyManagerBuilder<K2> keyMapper(Mapper<K2> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (JedisBasedProxyManagerBuilder<K2>) this;
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
        }

        public RedisApi getRedisApi() {
            return redisApi;
        }
    }

}
