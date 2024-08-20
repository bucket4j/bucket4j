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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.redisson.api.RFuture;
import org.redisson.client.RedisException;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.client.protocol.convertor.BooleanNotNullReplayConvertor;
import org.redisson.command.CommandAsyncExecutor;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.consts.LuaScripts;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson.RedissonBasedProxyManagerBuilder;
import io.netty.buffer.ByteBuf;

public class RedissonBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    public static final RedisCommand<Boolean> SET = new RedisCommand<>("SET", new BooleanNotNullReplayConvertor());

    private final CommandAsyncExecutor commandExecutor;
    private final ExpirationAfterWriteStrategy expirationStrategy;

    private final Mapper<K> keyMapper;

    public RedissonBasedProxyManager(RedissonBasedProxyManagerBuilder<K> builder) {
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
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        String stringKey = keyMapper.toString(key);
        List<Object> keys = Collections.singletonList(stringKey);
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                RFuture<byte[]> persistedState = commandExecutor.readAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.GET, stringKey);
                return Optional.ofNullable(getWithTimeout(persistedState, timeoutNanos));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
                if (ttlMillis > 0) {
                    if (originalData == null) {
                        // Redisson prohibits the usage null as values, so "replace" must not be used in such cases
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(stringKey, ByteArrayCodec.INSTANCE, SET, stringKey, encodeByteArray(newData), "PX", ttlMillis, "NX");
                        return getWithTimeout(redissonFuture, timeoutNanos);
                    } else {
                        Object[] params = new Object[] {originalData, newData, ttlMillis};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, LuaScripts.SCRIPT_COMPARE_AND_SWAP_PX, keys, params);
                        return getWithTimeout(redissonFuture, timeoutNanos);
                    }
                } else {
                    if (originalData == null) {
                        // Redisson prohibits the usage null as values, so "replace" must not be used in such cases
                        RFuture<Boolean> redissonFuture = commandExecutor.writeAsync(stringKey, ByteArrayCodec.INSTANCE, SET, stringKey, encodeByteArray(newData), "NX");
                        return getWithTimeout(redissonFuture, timeoutNanos);
                    } else {
                        Object[] params = new Object[] {originalData, newData};
                        RFuture<Boolean> redissonFuture = commandExecutor.evalWriteAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN, LuaScripts.SCRIPT_COMPARE_AND_SWAP, keys, params);
                        return getWithTimeout(redissonFuture, timeoutNanos);
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

    private <T> T getWithTimeout(RFuture<T> redissonFuture, Optional<Long> timeoutNanos) {
        if (timeoutNanos.isEmpty()) {
            return commandExecutor.get(redissonFuture);
        } else {
            try {
                return redissonFuture.get(timeoutNanos.get(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                redissonFuture.cancel(true);
                Thread.currentThread().interrupt();
                throw new RedisException(e);
            } catch (TimeoutException e) {
                String message = "Violated timeout while waiting for redis future for " + timeoutNanos.get() + "ns";
                throw new io.github.bucket4j.TimeoutException(message, timeoutNanos.get(), timeoutNanos.get());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RedisException re) {
                    throw re;
                }
                throw new RedisException(e);
            }
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
