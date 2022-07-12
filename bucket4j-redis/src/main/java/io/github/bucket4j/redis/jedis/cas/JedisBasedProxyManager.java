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

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class JedisBasedProxyManager extends AbstractCompareAndSwapBasedProxyManager<byte[]> {

    private final JedisPool jedisPool;
    private final long ttlMillis;

    public JedisBasedProxyManager(JedisPool jedisPool, ClientSideConfig clientSideConfig, Duration ttl) {
        super(clientSideConfig);
        Objects.requireNonNull(jedisPool);
        this.jedisPool = jedisPool;
        this.ttlMillis = ttl.toMillis();
    }

    public JedisBasedProxyManager(JedisPool jedisPool, Duration ttl) {
        this(jedisPool, ClientSideConfig.getDefault(), ttl);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(byte[] key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                return withResource(jedis -> Optional.ofNullable(jedis.get(key)));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData) {
                return compareAndSwapFuture(key, originalData, newData);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(byte[] key) {
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData() {
                return withResource(jedis -> CompletableFuture.supplyAsync(() -> Optional.ofNullable(jedis.get(key))));
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData) {
                return CompletableFuture.supplyAsync(() -> compareAndSwapFuture(key, originalData, newData));
            }
        };
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

    private final byte[] scriptSetNx = "return redis.call('set', KEYS[1], ARGV[1], 'nx', 'px', ARGV[2])".getBytes(StandardCharsets.UTF_8);
    private final byte[] scriptCompareAndSwap = (
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "redis.call('psetex', KEYS[1], ARGV[3], ARGV[2]); " +
                "return 1; " +
            "else " +
                "return 0; " +
            "end").getBytes(StandardCharsets.UTF_8);

    private Boolean compareAndSwapFuture(byte[] key, byte[] originalData, byte[] newData) {
        if (originalData == null) {
            // nulls are prohibited as values, so "replace" must not be used in such cases
            byte[][] keysAndArgs = {key, newData, encodeLong(ttlMillis)};
            Object res = withResource(jedis -> jedis.eval(scriptSetNx, 1, keysAndArgs));
            return res != null;
        } else {
            byte[][] keysAndArgs = {key, originalData, newData, encodeLong(ttlMillis)};
            Object res = withResource(jedis -> jedis.eval(scriptCompareAndSwap, 1, keysAndArgs));
            return res != null && !res.equals(0L);
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
}