package io.github.bucket4j.redis;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

/**
 * @author Vladimir Bukhtoyarov
 */
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

        specs = Arrays.asList(
            // Jedis
            new ProxyManagerSpec<>(
                "JedisBasedProxyManager_ByteArrayKey",
                () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                clientConfig -> JedisBasedProxyManager.builderFor(jedisPool)
                    .withClientSideConfig(clientConfig)
                    .build()
            ),
            new ProxyManagerSpec<>(
                "JedisBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                clientConfig -> JedisBasedProxyManager.builderFor(jedisPool)
                    .withClientSideConfig(clientConfig)
                    .withKeyMapper(Mapper.STRING)
                    .build()
            ),
            new ProxyManagerSpec<>(
                "JedisBasedProxyManager_unifiedJedisPooled_ByteArrayKey",
                () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                clientConfig -> JedisBasedProxyManager.builderFor(unifiedJedisPooled)
                    .withClientSideConfig(clientConfig)
                    .build()
            )
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
