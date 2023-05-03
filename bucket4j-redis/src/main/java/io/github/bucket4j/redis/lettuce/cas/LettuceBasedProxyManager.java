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

package io.github.bucket4j.redis.lettuce.cas;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.AbstractRedisProxyManagerBuilder;
import io.github.bucket4j.redis.consts.LuaScripts;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LettuceBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final RedisApi redisApi;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final Mapper<K> keyMapper;

    public static LettuceBasedProxyManagerBuilder<byte[]> builderFor(RedisAsyncCommands<byte[], byte[]> redisAsyncCommands) {
        Objects.requireNonNull(redisAsyncCommands);
        RedisApi redisApi = new RedisApi() {
            @Override
            public <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, byte[][] keys, byte[][] params) {
                return redisAsyncCommands.eval(script, scriptOutputType, keys, params);
            }
            @Override
            public RedisFuture<byte[]> get(byte[] key) {
                return redisAsyncCommands.get(key);
            }
            @Override
            public RedisFuture<Void> delete(byte[] key) {
                return (RedisFuture) redisAsyncCommands.del(key);
            }
        };
        return new LettuceBasedProxyManagerBuilder(Mapper.BYTES, redisApi);
    }

    public static LettuceBasedProxyManagerBuilder<byte[]> builderFor(StatefulRedisConnection<byte[], byte[]> statefulRedisConnection) {
        return builderFor(statefulRedisConnection.async());
    }

    public static LettuceBasedProxyManagerBuilder<byte[]> builderFor(RedisClient redisClient) {
        return builderFor(redisClient.connect(ByteArrayCodec.INSTANCE));
    }

    public static LettuceBasedProxyManagerBuilder<byte[]> builderFor(RedisClusterClient redisClient) {
        return builderFor(redisClient.connect(ByteArrayCodec.INSTANCE));
    }

    public static LettuceBasedProxyManagerBuilder<byte[]> builderFor(StatefulRedisClusterConnection<byte[], byte[]> connection) {
        return builderFor(connection.async());
    }

    public static LettuceBasedProxyManagerBuilder<byte[]> builderFor(RedisAdvancedClusterAsyncCommands<byte[], byte[]> redisAsyncCommands) {
        Objects.requireNonNull(redisAsyncCommands);
        RedisApi redisApi = new RedisApi() {
            @Override
            public <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, byte[][] keys, byte[][] params) {
                return redisAsyncCommands.eval(script, scriptOutputType, keys, params);
            }
            @Override
            public RedisFuture<byte[]> get(byte[] key) {
                return redisAsyncCommands.get(key);
            }
            @Override
            public RedisFuture<Void> delete(byte[] key) {
                return (RedisFuture) redisAsyncCommands.del(key);
            }
        };
        return new LettuceBasedProxyManagerBuilder<>(Mapper.BYTES, redisApi);
    }

    public static class LettuceBasedProxyManagerBuilder<K> extends AbstractRedisProxyManagerBuilder<LettuceBasedProxyManagerBuilder<K>> {

        private final RedisApi redisApi;
        private Mapper<K> keyMapper;

        private LettuceBasedProxyManagerBuilder(Mapper<K> keyMapper, RedisApi redisApi) {
            this.redisApi = Objects.requireNonNull(redisApi);
            this.keyMapper = keyMapper;
        }

        public <Key> LettuceBasedProxyManagerBuilder<Key> withKeyMapper(Mapper<Key> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (LettuceBasedProxyManagerBuilder) this;
        }

        public LettuceBasedProxyManager<K> build() {
            return new LettuceBasedProxyManager<K>(this);
        }

    }

    private LettuceBasedProxyManager(LettuceBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.expirationStrategy = builder.getNotNullExpirationStrategy();
        this.redisApi = builder.redisApi;
        this.keyMapper = builder.keyMapper;
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        byte[] keyBytes = keyMapper.toBytes(key);
        byte[][] keys = {keyBytes};
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                RedisFuture<byte[]> stateFuture = redisApi.get(keyBytes);
                return Optional.ofNullable(getFutureValue(stateFuture));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
                return getFutureValue(compareAndSwapFuture(keyBytes, keys, originalData, newData, newState));
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        byte[] keyBytes = keyMapper.toBytes(key);
        byte[][] keys = {keyBytes};
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData() {
                RedisFuture<byte[]> stateFuture = redisApi.get(keyBytes);
                return convertToCompletableFuture(stateFuture)
                        .thenApply((byte[] resultBytes) -> Optional.ofNullable(resultBytes));
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
                return convertToCompletableFuture(compareAndSwapFuture(keyBytes, keys, originalData, newData, newState));
            }
        };
    }

    @Override
    public void removeProxy(K key) {
        RedisFuture<?> future = redisApi.delete(keyMapper.toBytes(key));
        getFutureValue(future);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        RedisFuture<?> future = redisApi.delete(keyMapper.toBytes(key));
        return convertToCompletableFuture(future).thenApply(bytes -> null);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    private RedisFuture<Boolean> compareAndSwapFuture(byte[] key, byte[][] keys, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        long ttlMillis = calculateTtlMillis(newState);
        if (ttlMillis > 0) {
            if (originalData == null) {
                // nulls are prohibited as values, so "replace" must not be used in such cases
                byte[][] params = {newData, encodeLong(ttlMillis)};
                return redisApi.eval(LuaScripts.SCRIPT_SET_NX_PX, ScriptOutputType.BOOLEAN, keys, params);
            } else {
                byte[][] params = {originalData, newData, encodeLong(ttlMillis)};
                return redisApi.eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP_PX, ScriptOutputType.BOOLEAN, keys, params);
            }
        } else {
            if (originalData == null) {
                // nulls are prohibited as values, so "replace" must not be used in such cases
                byte[][] params = {newData};
                return redisApi.eval(LuaScripts.SCRIPT_SET_NX, ScriptOutputType.BOOLEAN, keys, params);
            } else {
                byte[][] params = {originalData, newData};
                return redisApi.eval(LuaScripts.SCRIPT_COMPARE_AND_SWAP, ScriptOutputType.BOOLEAN, keys, params);
            }
        }
    }

    private <T> CompletableFuture<T> convertToCompletableFuture(RedisFuture<T> redissonFuture) {
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

    private <V> V getFutureValue(RedisFuture<V> value) {
        try {
            return value.get();
        } catch (InterruptedException e) {
            value.cancel(true);
            Thread.currentThread().interrupt();
            throw new RedisException(e);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof RedisException ? (RedisException) e.getCause() :
                    new RedisException("Unexpected exception while processing command", e.getCause());
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

    private interface RedisApi {

        <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, byte[][] keys, byte[][] params);

        RedisFuture<byte[]> get(byte[] key);

        RedisFuture<Void> delete(byte[] key);

    }

}