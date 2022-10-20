/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2021 Vladimir Bukhtoyarov
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

package io.github.bucket4j.redis.redisson.cas;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.redis.AbstractRedisProxyManagerBuilder;
import io.github.bucket4j.redis.consts.LunaScripts;
import io.netty.buffer.ByteBuf;
import org.redisson.api.RFuture;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.client.protocol.convertor.BooleanNotNullReplayConvertor;
import org.redisson.command.CommandExecutor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RedissonBasedProxyManager extends AbstractCompareAndSwapBasedProxyManager<String> {

    public static RedisCommand<Boolean> SET = new RedisCommand<>("SET", new BooleanNotNullReplayConvertor());

    private final CommandExecutor commandExecutor;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    public static RedissonBasedProxyManagerBuilder builderFor(CommandExecutor commandExecutor) {
        return new RedissonBasedProxyManagerBuilder(commandExecutor);
    }

    public static class RedissonBasedProxyManagerBuilder extends AbstractRedisProxyManagerBuilder<RedissonBasedProxyManagerBuilder> {

        private final CommandExecutor commandExecutor;

        private RedissonBasedProxyManagerBuilder(CommandExecutor commandExecutor) {
            this.commandExecutor = Objects.requireNonNull(commandExecutor);
        }

        public RedissonBasedProxyManager build() {
            return new RedissonBasedProxyManager(this);
        }

    }

    private RedissonBasedProxyManager(RedissonBasedProxyManagerBuilder builder) {
        super(builder.getClientSideConfig());
        this.commandExecutor = builder.commandExecutor;
        this.expirationStrategy = builder.getNotNullExpirationStrategy();
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(String key) {
        List<Object> keys = Collections.singletonList(key);
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                byte[] persistedState = commandExecutor.read(key, ByteArrayCodec.INSTANCE, RedisCommands.GET, key);
                return Optional.ofNullable(persistedState);
            }
            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
                long ttlMillis = calculateTtlMillis(newState);
                if (ttlMillis > 0) {
                    if (originalData == null) {
                        // Redisson prohibits the usage null as values, so "replace" must not be used in such cases
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(key, ByteArrayCodec.INSTANCE, SET, key, encodeByteArray(newData), "PX", ttlMillis, "NX");
                        return commandExecutor.get(redissonFuture);
                    } else {
                        Object[] params = new Object[] {originalData, newData, ttlMillis};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(key, ByteArrayCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, LunaScripts.SCRIPT_COMPARE_AND_SWAP_PX, keys, params);
                        return commandExecutor.get(redissonFuture);
                    }
                } else {
                    if (originalData == null) {
                        // Redisson prohibits the usage null as values, so "replace" must not be used in such cases
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(key, ByteArrayCodec.INSTANCE, SET, key, encodeByteArray(newData), "NX");
                        return commandExecutor.get(redissonFuture);
                    } else {
                        Object[] params = new Object[] {originalData, newData};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(key, ByteArrayCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, LunaScripts.SCRIPT_COMPARE_AND_SWAP, keys, params);
                        return commandExecutor.get(redissonFuture);
                    }
                }
            }
        };
    }



    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(String key) {
        List<Object> keys = Collections.singletonList(key);
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData() {
                RFuture<byte[]> redissonFuture = commandExecutor.readAsync(key, ByteArrayCodec.INSTANCE, RedisCommands.GET, key);
                return convertFuture(redissonFuture)
                    .thenApply((byte[] resultBytes) -> Optional.ofNullable(resultBytes));
            }
            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
                long ttlMillis = calculateTtlMillis(newState);
                if (ttlMillis > 0) {
                    if (originalData == null) {
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(key, ByteArrayCodec.INSTANCE, SET, key, encodeByteArray(newData), "PX", ttlMillis, "NX");
                        return convertFuture(redissonFuture);
                    } else {
                        Object[] params = new Object[] {encodeByteArray(originalData), encodeByteArray(newData), ttlMillis};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(key, ByteArrayCodec.INSTANCE,
                                RedisCommands.EVAL_BOOLEAN, LunaScripts.SCRIPT_COMPARE_AND_SWAP_PX, keys, params);
                        return convertFuture(redissonFuture);
                    }
                } else {
                    if (originalData == null) {
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(key, ByteArrayCodec.INSTANCE, SET, key, encodeByteArray(newData), "NX");
                        return convertFuture(redissonFuture);
                    } else {
                        Object[] params = new Object[] {encodeByteArray(originalData), encodeByteArray(newData)};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(key, ByteArrayCodec.INSTANCE,
                                RedisCommands.EVAL_BOOLEAN, LunaScripts.SCRIPT_COMPARE_AND_SWAP, keys, params);
                        return convertFuture(redissonFuture);
                    }
                }
            }
        };
    }

    @Override
    public void removeProxy(String key) {
        RFuture<Object> future = commandExecutor.writeAsync(key, RedisCommands.DEL_VOID, key);
        commandExecutor.get(future);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(String key) {
        RFuture<?> redissonFuture = commandExecutor.writeAsync(key, RedisCommands.DEL_VOID, key);
        return convertFuture(redissonFuture).thenApply(bytes -> null);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    private <T> CompletableFuture<T> convertFuture(RFuture<T> redissonFuture) {
        CompletableFuture<T> jdkFuture = new CompletableFuture<>();
        redissonFuture.whenComplete((result, error) -> {
           if (error != null) {
               jdkFuture.completeExceptionally(error);
           } else {
               jdkFuture.complete(result);
           }
        });
        return jdkFuture;
    }

    public ByteBuf encodeByteArray(byte[] value) {
        try {
            return ByteArrayCodec.INSTANCE.getValueEncoder().encode(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private long calculateTtlMillis(RemoteBucketState state) {
        Optional<TimeMeter> clock = getClientSideConfig().getClientSideClock();
        long currentTimeNanos = clock.isPresent() ? clock.get().currentTimeNanos() : System.currentTimeMillis() * 1_000_000;
        return expirationStrategy.calculateTimeToLiveMillis(state, currentTimeNanos);
    }

}