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

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.redis.AbstractRedisProxyManagerBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class JedisBasedProxyManager extends AbstractCompareAndSwapBasedProxyManager<byte[]> {

    private final JedisPool jedisPool;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    public static JedisBasedProxyManagerBuilder builderFor(JedisPool jedisPool) {
        return new JedisBasedProxyManagerBuilder(jedisPool);
    }

    public static class JedisBasedProxyManagerBuilder extends AbstractRedisProxyManagerBuilder<JedisBasedProxyManagerBuilder> {

        private final JedisPool jedisPool;

        private JedisBasedProxyManagerBuilder(JedisPool jedisPool) {
            this.jedisPool = Objects.requireNonNull(jedisPool);
        }

        public JedisBasedProxyManager build() {
            return new JedisBasedProxyManager(this);
        }

    }

    private JedisBasedProxyManager(JedisBasedProxyManagerBuilder builder) {
        super(builder.getClientSideConfig());
        this.jedisPool = builder.jedisPool;
        this.expirationStrategy = builder.getNotNullExpirationStrategy();
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(byte[] key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                return withResource(jedis -> Optional.ofNullable(jedis.get(key)));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
                return JedisBasedProxyManager.this.compareAndSwap(key, originalData, newData, newState);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(byte[] key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProxy(byte[] key) {
        withResource(jedis -> jedis.del(key));
    }

    @Override
    protected CompletableFuture<Void> removeAsync(byte[] key) {
        return withResource(jedis -> CompletableFuture.runAsync(() -> jedis.del(key)));
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    private final byte[] scriptSetNxPx = "return redis.call('set', KEYS[1], ARGV[1], 'nx', 'px', ARGV[2])".getBytes(StandardCharsets.UTF_8);
    private final byte[] scriptCompareAndSwapPx = (
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "redis.call('psetex', KEYS[1], ARGV[3], ARGV[2]); " +
                "return 1; " +
            "else " +
                "return 0; " +
            "end").getBytes(StandardCharsets.UTF_8);
    private final byte[] scriptSetNx = "return redis.call('set', KEYS[1], ARGV[1], 'nx')".getBytes(StandardCharsets.UTF_8);
    private final byte[] scriptCompareAndSwap = (
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "redis.call('set', KEYS[1], ARGV[2]); " +
                    "return 1; " +
            "else " +
                    "return 0; " +
            "end").getBytes(StandardCharsets.UTF_8);

    private Boolean compareAndSwap(byte[] key, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        long ttlMillis = calculateTtlMillis(newState);
        if (ttlMillis > 0) {
            if (originalData == null) {
                // nulls are prohibited as values, so "replace" must not be used in such cases
                byte[][] keysAndArgs = {key, newData, encodeLong(ttlMillis)};
                Object res = withResource(jedis -> jedis.eval(scriptSetNxPx, 1, keysAndArgs));
                return res != null;
            } else {
                byte[][] keysAndArgs = {key, originalData, newData, encodeLong(ttlMillis)};
                Object res = withResource(jedis -> jedis.eval(scriptCompareAndSwapPx, 1, keysAndArgs));
                return res != null && !res.equals(0L);
            }
        } else {
            if (originalData == null) {
                // nulls are prohibited as values, so "replace" must not be used in such cases
                byte[][] keysAndArgs = {key, newData};
                Object res = withResource(jedis -> jedis.eval(scriptSetNx, 1, keysAndArgs));
                return res != null;
            } else {
                byte[][] keysAndArgs = {key, originalData, newData};
                Object res = withResource(jedis -> jedis.eval(scriptCompareAndSwap, 1, keysAndArgs));
                return res != null && !res.equals(0L);
            }
        }
    }

    private byte[] encodeLong(Long value) {
        return ("" + value).getBytes(StandardCharsets.UTF_8);
    }

    private <V> V withResource(Function<Jedis, V> fn) {
        try (Jedis jedis = jedisPool.getResource()) {
            return fn.apply(jedis);
        }
    }

    private long calculateTtlMillis(RemoteBucketState state) {
        Optional<TimeMeter> clock = getClientSideConfig().getClientSideClock();
        long currentTimeNanos = clock.isPresent() ? clock.get().currentTimeNanos() : System.currentTimeMillis() * 1_000_000;
        return expirationStrategy.calculateTimeToLiveMillis(state, currentTimeNanos);
    }

}