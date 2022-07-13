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
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.ReturnType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SpringDataRedisBasedProxyManager extends AbstractCompareAndSwapBasedProxyManager<byte[]> {

    private final RedisCommands commands;
    private final long keepAfterRefillDurationMillis;
    private final boolean isAsyncModeSupported;

    /**
     *
     * @param redisCommands
     * @param clientSideConfig
     * @param keepAfterRefillDuration
     * @param isAsyncModeSupported specifies how long bucket should be held in the cache after all consumed tokens have been refilled.
     */
    public SpringDataRedisBasedProxyManager(RedisCommands redisCommands, ClientSideConfig clientSideConfig, Duration keepAfterRefillDuration, boolean isAsyncModeSupported) {
        super(clientSideConfig);
        Objects.requireNonNull(redisCommands);
        this.commands = redisCommands;
        this.keepAfterRefillDurationMillis = keepAfterRefillDuration.toMillis();
        this.isAsyncModeSupported = isAsyncModeSupported;
    }

    /**
     *
     * @param redisCommands
     * @param clientSideConfig
     * @param keepAfterRefillDuration specifies how long bucket should be held in the cache after all consumed tokens have been refilled.
     */
    public SpringDataRedisBasedProxyManager(RedisCommands redisCommands, ClientSideConfig clientSideConfig, Duration keepAfterRefillDuration) {
        this(redisCommands, clientSideConfig, keepAfterRefillDuration, true);
    }

    /**
     * @param redisCommands
     * @param keepAfterRefillDuration specifies how long bucket should be held in the cache after all consumed tokens have been refilled.
     */
    public SpringDataRedisBasedProxyManager(RedisCommands redisCommands, Duration keepAfterRefillDuration) {
        this(redisCommands, ClientSideConfig.getDefault(), keepAfterRefillDuration);
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
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData() {
                return CompletableFuture.supplyAsync(() -> Optional.ofNullable(commands.get(key)));
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
                return CompletableFuture.supplyAsync(() -> SpringDataRedisBasedProxyManager.this.compareAndSwap(key, originalData, newData, newState));
            }
        };
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
        return isAsyncModeSupported;
    }

    private final byte[] scriptSetNx = "return redis.call('set', KEYS[1], ARGV[1], 'nx', 'px', ARGV[2])".getBytes(StandardCharsets.UTF_8);
    private final byte[] scriptCompareAndSwap = (
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "redis.call('psetex', KEYS[1], ARGV[3], ARGV[2]); " +
                "return 1; " +
            "else " +
                "return 0; " +
            "end").getBytes(StandardCharsets.UTF_8);

    private Boolean compareAndSwap(byte[] key, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        if (originalData == null) {
            // nulls are prohibited as values, so "replace" must not be used in such cases
            byte[][] keysAndArgs = {key, newData, encodeLong(calculateTtlMillis(newState))};
            return commands.eval(scriptSetNx, ReturnType.BOOLEAN, 1, keysAndArgs);
        } else {
            byte[][] keysAndArgs = {key, originalData, newData, encodeLong(calculateTtlMillis(newState))};
            return commands.eval(scriptCompareAndSwap, ReturnType.BOOLEAN, 1, keysAndArgs);
        }
    }

    private byte[] encodeLong(Long value) {
        return ("" + value).getBytes(StandardCharsets.UTF_8);
    }

    private long calculateTtlMillis(RemoteBucketState state) {
        Optional<TimeMeter> clock = getClientSideConfig().getClientSideClock();
        long currentTimeNanos = clock.isPresent() ? clock.get().currentTimeNanos() : System.currentTimeMillis() * 1_000_000;
        long millisToFullRefill = state.calculateFullRefillingTime(currentTimeNanos) / 1_000_000;
        return keepAfterRefillDurationMillis + millisToFullRefill;
    }
}