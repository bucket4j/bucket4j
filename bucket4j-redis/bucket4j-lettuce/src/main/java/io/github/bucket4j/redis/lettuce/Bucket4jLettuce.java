package io.github.bucket4j.redis.lettuce;

import java.util.Objects;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;

/**
 * Entry point for Lettuce integration
 */
public class Bucket4jLettuce {


    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param redisAsyncCommands
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static <K> LettuceBasedProxyManagerBuilder<K> builderFor(RedisAsyncCommands<K, byte[]> redisAsyncCommands) {
        Objects.requireNonNull(redisAsyncCommands);
        RedisApi<K> redisApi = new RedisApi<>() {
            @Override
            public <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, K[] keys, byte[][] params) {
                return redisAsyncCommands.eval(script, scriptOutputType, keys, params);
            }
            @Override
            public RedisFuture<byte[]> get(K key) {
                return redisAsyncCommands.get(key);
            }
            @Override
            public RedisFuture<?> delete(K key) {
                return redisAsyncCommands.del(key);
            }
        };
        return new LettuceBasedProxyManagerBuilder<>(redisApi);
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param statefulRedisConnection
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static <K> LettuceBasedProxyManagerBuilder<K> builderFor(StatefulRedisConnection<K, byte[]> statefulRedisConnection) {
        return builderFor(statefulRedisConnection.async());
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param redisClient
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static LettuceBasedProxyManagerBuilder<byte[]> builderFor(RedisClient redisClient) {
        return builderFor(redisClient.connect(ByteArrayCodec.INSTANCE));
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param redisClient
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static LettuceBasedProxyManagerBuilder<byte[]> builderFor(RedisClusterClient redisClient) {
        return builderFor(redisClient.connect(ByteArrayCodec.INSTANCE));
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param connection
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static <K> LettuceBasedProxyManagerBuilder<K> builderFor(StatefulRedisClusterConnection<K, byte[]> connection) {
        return builderFor(connection.async());
    }

    /**
     * Returns the builder for {@link LettuceBasedProxyManager}
     *
     * @param redisAsyncCommands
     *
     * @return new instance of {@link LettuceBasedProxyManagerBuilder}
     */
    public static <K> LettuceBasedProxyManagerBuilder<K> builderFor(RedisAdvancedClusterAsyncCommands<K, byte[]> redisAsyncCommands) {
        Objects.requireNonNull(redisAsyncCommands);
        RedisApi<K> redisApi = new RedisApi<>() {
            @Override
            public <V> RedisFuture<V> eval(String script, ScriptOutputType scriptOutputType, K[] keys, byte[][] params) {
                return redisAsyncCommands.eval(script, scriptOutputType, keys, params);
            }
            @Override
            public RedisFuture<byte[]> get(K key) {
                return redisAsyncCommands.get(key);
            }
            @Override
            public RedisFuture<?> delete(K key) {
                return redisAsyncCommands.del(key);
            }
        };
        return new LettuceBasedProxyManagerBuilder<>(redisApi);
    }

    public static class LettuceBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, LettuceBasedProxyManager<K>, LettuceBasedProxyManagerBuilder<K>> {

        private final RedisApi<K> redisApi;

        public LettuceBasedProxyManagerBuilder(RedisApi<K> redisApi) {
            this.redisApi = redisApi;
        }

        public RedisApi<K> getRedisApi() {
            return redisApi;
        }

        @Override
        public LettuceBasedProxyManager<K> build() {
            return new LettuceBasedProxyManager<>(this);
        }
    }

}
