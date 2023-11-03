package io.github.bucket4j.redis.jedis.cas;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class JedisProxyManager_UnifiedJedis_JedisPooled_Test extends AbstractDistributedBucketTest<byte[]> {
    private static GenericContainer container;
    private static UnifiedJedis unifiedJedis;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();
        unifiedJedis = createUnifiedJedisClient(container);
    }

    @AfterAll
    public static void shutdown() {
        if (unifiedJedis != null) {
            unifiedJedis.close();
        }
        if (container != null) {
            container.close();
        }
    }

    private static UnifiedJedis createUnifiedJedisClient(GenericContainer container) {
        String redisHost = container.getHost();
        Integer redisPort = container.getMappedPort(6379);

        return new JedisPooled(redisHost, redisPort);
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:7.0.2").withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    @Override
    protected ProxyManager<byte[]> getProxyManager() {
        return JedisBasedProxyManager.builderFor(unifiedJedis)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.none())
                .build();
    }

    @Override
    protected byte[] generateRandomKey() {
        return UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    }
}
