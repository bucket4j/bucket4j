package io.github.bucket4j.redis;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.jedis.Bucket4jJedis;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

public class JedisBasedProxyManagerStandaloneTest extends AbstractDistributedBucketTest {

    private static GenericContainer container;

    // jedis
    private static JedisPool jedisPool;

    private static UnifiedJedis unifiedJedis;
    private static UnifiedJedis unifiedJedisPooled;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();

        // jedis
        jedisPool = createJedisClient(container);
        unifiedJedisPooled = createUnifiedJedisPooledClient(container);
        unifiedJedis = createUnifiedJedisClient(container);

        BackwardCompatibilityStateCheckHelper<byte[]> byteArrayKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(byte[] key) {
                try (Jedis jedis = jedisPool.getResource()) {
                    return jedis.get(key);
                }
            }

            @Override
            public void setRawState(byte[] key, byte[] state) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.set(key, state);
                }
            }
        };

        BackwardCompatibilityStateCheckHelper<String> stringKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                try (Jedis jedis = jedisPool.getResource()) {
                    return jedis.get(key.getBytes(StandardCharsets.UTF_8));
                }
            }

            @Override
            public void setRawState(String key, byte[] state) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.set(key.getBytes(StandardCharsets.UTF_8), state);
                }
            }
        };

        BackwardCompatibilityStateCheckHelper<byte[]> unifiedJedisPooledByteArrayKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(byte[] key) {
                return unifiedJedisPooled.get(key);
            }

            @Override
            public void setRawState(byte[] key, byte[] state) {
                unifiedJedisPooled.set(key, state);
            }
        };

        specs = Arrays.asList(
            // Jedis
            new ProxyManagerSpec<>(
                "JedisBasedProxyManager_ByteArrayKey",
                () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                () -> Bucket4jJedis.casBasedBuilder(jedisPool)
            ).checkExpiration().checkStateBackwardCompatibility(byteArrayKeyHelper),
            new ProxyManagerSpec<>(
                "JedisBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jJedis.casBasedBuilder(jedisPool).keyMapper(Mapper.STRING)
            ).checkExpiration().checkStateBackwardCompatibility(stringKeyHelper),
            new ProxyManagerSpec<>(
                "JedisBasedProxyManager_unifiedJedisPooled_ByteArrayKey",
                () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                () -> Bucket4jJedis.casBasedBuilder(unifiedJedisPooled)
            ).checkExpiration().checkStateBackwardCompatibility(unifiedJedisPooledByteArrayKeyHelper)
        );
    }

    @AfterAll
    public static void shutdown() {
        try {
            try {
                try {
                    if (jedisPool != null) {
                        jedisPool.close();
                    }
                } finally {
                    if (unifiedJedis != null) {
                        unifiedJedis.close();
                    }
                }
            } finally {
                if (unifiedJedisPooled != null) {
                    unifiedJedisPooled.close();
                }
            }
        } finally {
            if (container != null) {
                container.close();
            }
        }
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:4.0.11")
            .withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    private static JedisPool createJedisClient(GenericContainer container) {
        String redisHost = container.getHost();
        Integer redisPort = container.getMappedPort(6379);

        return new JedisPool(redisHost, redisPort);
    }

    private static UnifiedJedis createUnifiedJedisPooledClient(GenericContainer container) {
        String redisHost = container.getHost();
        Integer redisPort = container.getMappedPort(6379);

        return new JedisPooled(redisHost, redisPort);
    }

    private static UnifiedJedis createUnifiedJedisClient(GenericContainer container) {
        String redisHost = container.getHost();
        Integer redisPort = container.getMappedPort(6379);

        return new UnifiedJedis(HostAndPort.from(redisHost + ":" + redisPort));
    }

}
