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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RFuture;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.client.protocol.convertor.BooleanNotNullReplayConvertor;
import org.redisson.command.CommandAsyncExecutor;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractAsyncCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.consts.LuaScripts;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson.RedissonAsyncProxyManagerBuilder;
import io.netty.buffer.ByteBuf;

public class RedissonAsyncProxyManager<K> extends AbstractAsyncCompareAndSwapBasedProxyManager<K> {

    public static final RedisCommand<Boolean> SET = new RedisCommand<>("SET", new BooleanNotNullReplayConvertor());

    private final CommandAsyncExecutor commandExecutor;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final Mapper<K> keyMapper;

    public RedissonAsyncProxyManager(RedissonAsyncProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        this.commandExecutor = builder.getCommandExecutor();
        this.expirationStrategy = builder.getExpirationAfterWrite().orElse(ExpirationAfterWriteStrategy.none());
        this.keyMapper = builder.getKeyMapper();
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        String stringKey = keyMapper.toString(key);
        List<Object> keys = Collections.singletonList(stringKey);
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                RFuture<byte[]> redissonFuture = commandExecutor.readAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.GET, stringKey);
                if (timeoutNanos.isEmpty()) {
                    return convertFuture(redissonFuture, timeoutNanos)
                        .thenApply((byte[] resultBytes) -> Optional.ofNullable(resultBytes));
                } else {
                    return convertFuture(redissonFuture, timeoutNanos)
                        .thenApply((byte[] resultBytes) -> Optional.ofNullable(resultBytes));
                }
            }
            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
                if (ttlMillis > 0) {
                    if (originalData == null) {
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(stringKey, ByteArrayCodec.INSTANCE, SET, stringKey, encodeByteArray(newData), "PX", ttlMillis, "NX");
                        return convertFuture(redissonFuture, timeoutNanos);
                    } else {
                        Object[] params = new Object[] {encodeByteArray(originalData), encodeByteArray(newData), ttlMillis};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(stringKey, ByteArrayCodec.INSTANCE,
                                RedisCommands.EVAL_BOOLEAN, LuaScripts.SCRIPT_COMPARE_AND_SWAP_PX, keys, params);
                        return convertFuture(redissonFuture, timeoutNanos);
                    }
                } else {
                    if (originalData == null) {
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(stringKey, ByteArrayCodec.INSTANCE, SET, stringKey, encodeByteArray(newData), "NX");
                        return convertFuture(redissonFuture, timeoutNanos);
                    } else {
                        Object[] params = new Object[] {encodeByteArray(originalData), encodeByteArray(newData)};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(stringKey, ByteArrayCodec.INSTANCE,
                                RedisCommands.EVAL_BOOLEAN, LuaScripts.SCRIPT_COMPARE_AND_SWAP, keys, params);
                        return convertFuture(redissonFuture, timeoutNanos);
                    }
                }
            }
        };
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        RFuture<?> redissonFuture = commandExecutor.writeAsync(keyMapper.toString(key), RedisCommands.DEL_VOID, key);
        return convertFuture(redissonFuture, Optional.empty()).thenApply(bytes -> null);
    }

    private <T> CompletableFuture<T> convertFuture(RFuture<T> redissonFuture, Optional<Long> timeoutNanos) {
        if (timeoutNanos.isEmpty()) {
            return redissonFuture.toCompletableFuture();
        } else {
            return redissonFuture.toCompletableFuture().orTimeout(timeoutNanos.get(), TimeUnit.NANOSECONDS);
        }
    }

    public ByteBuf encodeByteArray(byte[] value) {
        try {
            return ByteArrayCodec.INSTANCE.getValueEncoder().encode(value);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
