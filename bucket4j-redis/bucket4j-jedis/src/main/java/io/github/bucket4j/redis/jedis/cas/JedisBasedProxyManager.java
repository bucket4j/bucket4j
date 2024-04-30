/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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

package io.github.bucket4j.redis.jedis.cas;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.AbstractRedisProxyManagerBuilder;
import io.github.bucket4j.redis.consts.LuaScripts;
import io.github.bucket4j.redis.jedis.Bucket4jJedis;
import io.github.bucket4j.redis.jedis.RedisApi;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.util.Pool;

public class JedisBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final RedisApi redisApi;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final Mapper<K> keyMapper;

    /**
     * @deprecated use {@link Bucket4jJedis#casBasedBuilder(Pool)}
     */
    @Deprecated
    public static JedisBasedProxyManagerBuilder<byte[]> builderFor(Pool<Jedis> jedisPool) {
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
     * @deprecated use {@link Bucket4jJedis#casBasedBuilder(UnifiedJedis)}
     */
    @Deprecated
    public static JedisBasedProxyManagerBuilder<byte[]> builderFor(UnifiedJedis unifiedJedis) {
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
     * @deprecated use {@link Bucket4jJedis#casBasedBuilder(JedisCluster)}
     */
    public static JedisBasedProxyManagerBuilder<byte[]> builderFor(JedisCluster jedisCluster) {
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

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    public static class JedisBasedProxyManagerBuilder<K> extends AbstractRedisProxyManagerBuilder<JedisBasedProxyManagerBuilder<K>> {

        private final RedisApi redisApi;
        private Mapper<K> keyMapper;

        public <Key> JedisBasedProxyManagerBuilder<Key> withKeyMapper(Mapper<Key> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (JedisBasedProxyManagerBuilder) this;
        }

        private JedisBasedProxyManagerBuilder(Mapper<K> keyMapper, RedisApi redisApi) {
            this.keyMapper = Objects.requireNonNull(keyMapper);
            this.redisApi = Objects.requireNonNull(redisApi);
        }

        public JedisBasedProxyManager<K> build() {
            return new JedisBasedProxyManager<>(this);
        }

    }

    public JedisBasedProxyManager(Bucket4jJedis.JedisBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.keyMapper = builder.getKeyMapper();
        this.expirationStrategy = builder.getExpirationAfterWrite().orElse(ExpirationAfterWriteStrategy.none());
        this.redisApi = builder.getRedisApi();
    }

    private JedisBasedProxyManager(JedisBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.redisApi = builder.redisApi;
        this.expirationStrategy = builder.getNotNullExpirationStrategy();
        this.keyMapper = builder.keyMapper;
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        byte[] keyBytes = keyMapper.toBytes(key);
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                return Optional.ofNullable(redisApi.get(keyBytes));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                return JedisBasedProxyManager.this.compareAndSwap(keyBytes, originalData, newData, newState);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProxy(K key) {
        redisApi.delete(keyMapper.toBytes(key));
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    private Boolean compareAndSwap(byte[] key, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
        if (ttlMillis > 0) {
            if (originalData == null) {
                // nulls are prohibited as values, so "replace" must not be used in such cases
                byte[][] keysAndArgs = {key, newData, encodeLong(ttlMillis)};
                Object res = redisApi.eval(LuaScripts.SCRIPT_SET_NX_PX.getBytes(StandardCharsets.UTF_8), 1, keysAndArgs);
                return res != null && !res.equals(0L);
            } else {
                byte[][] keysAndArgs = {key, originalData, newData, encodeLong(ttlMillis)};
                Object res = redisApi.eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP_PX.getBytes(StandardCharsets.UTF_8), 1, keysAndArgs);
                return res != null && !res.equals(0L);
            }
        } else {
            if (originalData == null) {
                // nulls are prohibited as values, so "replace" must not be used in such cases
                byte[][] keysAndArgs = {key, newData};
                Object res = redisApi.eval(LuaScripts.SCRIPT_SET_NX.getBytes(StandardCharsets.UTF_8), 1, keysAndArgs);
                return res != null && !res.equals(0L);
            } else {
                byte[][] keysAndArgs = {key, originalData, newData};
                Object res = redisApi.eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP.getBytes(StandardCharsets.UTF_8), 1, keysAndArgs);
                return res != null && !res.equals(0L);
            }
        }
    }

    private byte[] encodeLong(Long value) {
        return ("" + value).getBytes(StandardCharsets.UTF_8);
    }

}
