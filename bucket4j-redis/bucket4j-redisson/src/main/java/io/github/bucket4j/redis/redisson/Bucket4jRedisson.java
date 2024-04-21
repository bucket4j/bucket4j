/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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
package io.github.bucket4j.redis.redisson;

import java.util.Objects;

import org.redisson.command.CommandAsyncExecutor;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;

/**
 * Entry point for Jedis integration
 */
public class Bucket4jRedisson {

    /**
     * Returns the builder for {@link RedissonBasedProxyManager}
     *
     * @param commandExecutor
     *
     * @return new instance of {@link RedissonBasedProxyManagerBuilder}
     */
    public static RedissonBasedProxyManagerBuilder<String> casBasedBuilder(CommandAsyncExecutor commandExecutor) {
        return new RedissonBasedProxyManagerBuilder<>(Mapper.STRING, commandExecutor);
    }

    public static class RedissonBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, RedissonBasedProxyManager<K>, RedissonBasedProxyManagerBuilder<K>> {

        private final CommandAsyncExecutor commandExecutor;
        private Mapper<K> keyMapper;

        public RedissonBasedProxyManagerBuilder(Mapper<K> keyMapper, CommandAsyncExecutor commandExecutor) {
            this.commandExecutor = Objects.requireNonNull(commandExecutor);
            this.keyMapper = Objects.requireNonNull(keyMapper);
        }

        /**
         * Specifies the type of key.
         *
         * @param keyMapper object responsible for converting primary keys to byte arrays.
         *
         * @return this builder instance
         */
        public <K2> RedissonBasedProxyManagerBuilder<K2> keyMapper(Mapper<K2> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (RedissonBasedProxyManagerBuilder<K2>) this;
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
        }

        public CommandAsyncExecutor getCommandExecutor() {
            return commandExecutor;
        }

        @Override
        public RedissonBasedProxyManager<K> build() {
            return new RedissonBasedProxyManager<>(this);
        }

    }

}
