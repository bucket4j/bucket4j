/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
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
package io.github.bucket4j.redis.vertx;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.vertx.cas.VertxBasedProxyManager;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Entry point for Vert.x integration
 */
public class Bucket4jVertx {

    /**
     * Returns the builder for {@link VertxBasedProxyManager}
     *
     * @param redis
     *
     * @return new instance of {@link VertxBasedProxyManagerBuilder}
     */
    public static VertxBasedProxyManagerBuilder<byte[]> casBasedBuilder(Redis redis) {
        Objects.requireNonNull(redis, "redis");
        RedisApi redisApi = new RedisApi() {
            @Override
            public CompletableFuture<Boolean> eval(String script, byte[][] keys, byte[][] params) {
                Request request = Request.cmd(Command.EVAL)
                    .arg(script)
                    .arg(keys.length);
                for (byte[] key : keys) {
                    request.arg(key);
                }
                for (byte[] param : params) {
                    request.arg(param);
                }
                return redis.send(request)
                    .toCompletionStage()
                    .toCompletableFuture()
                    .thenApply(Bucket4jVertx::toBooleanResult);
            }

            @Override
            public CompletableFuture<byte[]> get(byte[] key) {
                Request request = Request.cmd(Command.GET).arg(key);
                return redis.send(request)
                    .toCompletionStage()
                    .toCompletableFuture()
                    .thenApply(response -> response == null ? null : response.toBytes());
            }

            @Override
            public CompletableFuture<Void> delete(byte[] key) {
                Request request = Request.cmd(Command.DEL).arg(key);
                return redis.send(request)
                    .toCompletionStage()
                    .toCompletableFuture()
                    .thenApply(ignored -> null);
            }
        };
        return new VertxBasedProxyManagerBuilder<>(Mapper.BYTES, redisApi);
    }

    private static boolean toBooleanResult(Response response) {
        if (response == null) {
            return false;
        }

        try {
            Long longValue = response.toLong();
            if (longValue != null) {
                return longValue != 0L;
            }
        } catch (RuntimeException ignored) {
        }

        String textValue = response.toString();
        return "1".equals(textValue) || "OK".equalsIgnoreCase(textValue) || "TRUE".equalsIgnoreCase(textValue);
    }

    public static class VertxBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, VertxBasedProxyManager<K>, VertxBasedProxyManagerBuilder<K>> {

        private final RedisApi redisApi;
        private Mapper<K> keyMapper;

        public VertxBasedProxyManagerBuilder(Mapper<K> keyMapper, RedisApi redisApi) {
            this.redisApi = Objects.requireNonNull(redisApi, "redisApi");
            this.keyMapper = Objects.requireNonNull(keyMapper, "keyMapper");
        }

        /**
         * Specifies the type of key.
         *
         * @param keyMapper object responsible for converting keys to byte arrays
         *
         * @return this builder instance
         */
        @SuppressWarnings("unchecked")
        public <K2> VertxBasedProxyManagerBuilder<K2> keyMapper(Mapper<K2> keyMapper) {
            this.keyMapper = (Mapper<K>) Objects.requireNonNull(keyMapper, "keyMapper");
            return (VertxBasedProxyManagerBuilder<K2>) this;
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
        }

        public RedisApi getRedisApi() {
            return redisApi;
        }

        @Override
        public VertxBasedProxyManager<K> build() {
            return new VertxBasedProxyManager<>(this);
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }
    }

}
