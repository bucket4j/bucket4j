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
    public static RedissonBasedProxyManagerBuilder<String> builderFor(CommandAsyncExecutor commandExecutor) {
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
