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
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.AbstractRedisProxyManagerBuilder;
import io.github.bucket4j.redis.consts.LuaScripts;
import io.netty.buffer.ByteBuf;
import org.redisson.api.RFuture;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.client.protocol.convertor.BooleanNotNullReplayConvertor;
import org.redisson.command.CommandAsyncExecutor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RedissonBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    public static RedisCommand<Boolean> SET = new RedisCommand<>("SET", new BooleanNotNullReplayConvertor());

    private final CommandAsyncExecutor commandExecutor;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    private final Mapper<K> keyMapper;

    public static RedissonBasedProxyManagerBuilder<String> builderFor(CommandAsyncExecutor commandExecutor) {
        return new RedissonBasedProxyManagerBuilder<>(Mapper.STRING, commandExecutor);
    }

    public static class RedissonBasedProxyManagerBuilder<K> extends AbstractRedisProxyManagerBuilder<RedissonBasedProxyManagerBuilder<K>> {

        private final CommandAsyncExecutor commandExecutor;
        private Mapper<K> keyMapper;

        private RedissonBasedProxyManagerBuilder(Mapper<K> keyMapper, CommandAsyncExecutor commandExecutor) {
            this.keyMapper = Objects.requireNonNull(keyMapper);
            this.commandExecutor = Objects.requireNonNull(commandExecutor);
        }

        public <Key> RedissonBasedProxyManagerBuilder<Key> withKeyMapper(Mapper<Key> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (RedissonBasedProxyManagerBuilder) this;
        }

        public RedissonBasedProxyManager<K> build() {
            return new RedissonBasedProxyManager<>(this);
        }

    }

    private RedissonBasedProxyManager(RedissonBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.commandExecutor = builder.commandExecutor;
        this.expirationStrategy = builder.getNotNullExpirationStrategy();
        this.keyMapper = builder.keyMapper;
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        String stringKey = keyMapper.toString(key);
        List<Object> keys = Collections.singletonList(stringKey);
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                RFuture<byte[]> persistedState = commandExecutor.readAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.GET, stringKey);
                return Optional.ofNullable(commandExecutor.get(persistedState));
            }
            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
                long ttlMillis = calculateTtlMillis(newState);
                if (ttlMillis > 0) {
                    if (originalData == null) {
                        // Redisson prohibits the usage null as values, so "replace" must not be used in such cases
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(stringKey, ByteArrayCodec.INSTANCE, SET, stringKey, encodeByteArray(newData), "PX", ttlMillis, "NX");
                        return commandExecutor.get(redissonFuture);
                    } else {
                        Object[] params = new Object[] {originalData, newData, ttlMillis};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, LuaScripts.SCRIPT_COMPARE_AND_SWAP_PX, keys, params);
                        return commandExecutor.get(redissonFuture);
                    }
                } else {
                    if (originalData == null) {
                        // Redisson prohibits the usage null as values, so "replace" must not be used in such cases
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(stringKey, ByteArrayCodec.INSTANCE, SET, stringKey, encodeByteArray(newData), "NX");
                        return commandExecutor.get(redissonFuture);
                    } else {
                        Object[] params = new Object[] {originalData, newData};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, LuaScripts.SCRIPT_COMPARE_AND_SWAP, keys, params);
                        return commandExecutor.get(redissonFuture);
                    }
                }
            }
        };
    }



    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        String stringKey = keyMapper.toString(key);
        List<Object> keys = Collections.singletonList(stringKey);
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData() {
                RFuture<byte[]> redissonFuture = commandExecutor.readAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.GET, stringKey);
                return convertFuture(redissonFuture)
                    .thenApply((byte[] resultBytes) -> Optional.ofNullable(resultBytes));
            }
            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
                long ttlMillis = calculateTtlMillis(newState);
                if (ttlMillis > 0) {
                    if (originalData == null) {
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(stringKey, ByteArrayCodec.INSTANCE, SET, stringKey, encodeByteArray(newData), "PX", ttlMillis, "NX");
                        return convertFuture(redissonFuture);
                    } else {
                        Object[] params = new Object[] {encodeByteArray(originalData), encodeByteArray(newData), ttlMillis};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(stringKey, ByteArrayCodec.INSTANCE,
                                RedisCommands.EVAL_BOOLEAN, LuaScripts.SCRIPT_COMPARE_AND_SWAP_PX, keys, params);
                        return convertFuture(redissonFuture);
                    }
                } else {
                    if (originalData == null) {
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(stringKey, ByteArrayCodec.INSTANCE, SET, stringKey, encodeByteArray(newData), "NX");
                        return convertFuture(redissonFuture);
                    } else {
                        Object[] params = new Object[] {encodeByteArray(originalData), encodeByteArray(newData)};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(stringKey, ByteArrayCodec.INSTANCE,
                                RedisCommands.EVAL_BOOLEAN, LuaScripts.SCRIPT_COMPARE_AND_SWAP, keys, params);
                        return convertFuture(redissonFuture);
                    }
                }
            }
        };
    }

    @Override
    public void removeProxy(K key) {
        RFuture<Object> future = commandExecutor.writeAsync(keyMapper.toString(key), RedisCommands.DEL_VOID, key);
        commandExecutor.get(future);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        RFuture<?> redissonFuture = commandExecutor.writeAsync(keyMapper.toString(key), RedisCommands.DEL_VOID, key);
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
