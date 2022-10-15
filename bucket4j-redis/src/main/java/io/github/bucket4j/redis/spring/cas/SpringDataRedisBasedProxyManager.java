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

package io.github.bucket4j.redis.spring.cas;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.redis.AbstractRedisProxyManagerBuilder;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.ReturnType;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SpringDataRedisBasedProxyManager extends AbstractCompareAndSwapBasedProxyManager<byte[]> {

    private final RedisCommands commands;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    public static SpringDataRedisBasedProxyManagerBuilder builderFor(RedisCommands redisCommands) {
        return new SpringDataRedisBasedProxyManagerBuilder(redisCommands);
    }

    public static class SpringDataRedisBasedProxyManagerBuilder extends AbstractRedisProxyManagerBuilder<SpringDataRedisBasedProxyManagerBuilder> {

        private final RedisCommands redisCommands;

        private SpringDataRedisBasedProxyManagerBuilder(RedisCommands redisCommands) {
            this.redisCommands = Objects.requireNonNull(redisCommands);
        }

        public SpringDataRedisBasedProxyManager build() {
            return new SpringDataRedisBasedProxyManager(this);
        }

    }

    private SpringDataRedisBasedProxyManager(SpringDataRedisBasedProxyManagerBuilder builder) {
        super(builder.getClientSideConfig());
        this.commands = builder.redisCommands;
        this.expirationStrategy = builder.getNotNullExpirationStrategy();
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(byte[] key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                return Optional.ofNullable(commands.get(key));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
                return SpringDataRedisBasedProxyManager.this.compareAndSwap(key, originalData, newData, newState);
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(byte[] key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProxy(byte[] key) {
        commands.del(key);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(byte[] key) {
        return CompletableFuture.runAsync(() -> commands.del(key));
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    private final byte[] scriptSetNxPx = "return redis.call('set', KEYS[1], ARGV[1], 'nx', 'px', ARGV[2])".getBytes(StandardCharsets.UTF_8);
    private final byte[] scriptSetNx = "return redis.call('set', KEYS[1], ARGV[1], 'nx')".getBytes(StandardCharsets.UTF_8);
    private final byte[] scriptCompareAndSwapPx = (
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "redis.call('psetex', KEYS[1], ARGV[3], ARGV[2]); " +
                "return 1; " +
            "else " +
                "return 0; " +
            "end").getBytes(StandardCharsets.UTF_8);
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
                return commands.eval(scriptSetNxPx, ReturnType.BOOLEAN, 1, keysAndArgs);
            } else {
                byte[][] keysAndArgs = {key, originalData, newData, encodeLong(ttlMillis)};
                return commands.eval(scriptCompareAndSwapPx, ReturnType.BOOLEAN, 1, keysAndArgs);
            }
        } else {
            if (originalData == null) {
                // nulls are prohibited as values, so "replace" must not be used in such cases
                byte[][] keysAndArgs = {key, newData};
                return commands.eval(scriptSetNx, ReturnType.BOOLEAN, 1, keysAndArgs);
            } else {
                byte[][] keysAndArgs = {key, originalData, newData};
                return commands.eval(scriptCompareAndSwap, ReturnType.BOOLEAN, 1, keysAndArgs);
            }
        }
    }

    private byte[] encodeLong(Long value) {
        return ("" + value).getBytes(StandardCharsets.UTF_8);
    }

    private long calculateTtlMillis(RemoteBucketState state) {
        Optional<TimeMeter> clock = getClientSideConfig().getClientSideClock();
        long currentTimeNanos = clock.isPresent() ? clock.get().currentTimeNanos() : System.currentTimeMillis() * 1_000_000;
        return expirationStrategy.calculateTimeToLiveMillis(state, currentTimeNanos);
    }
}
