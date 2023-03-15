package io.github.bucket4j.redis.jedis.cas;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class JedisBasedProxyManager_StringKey_Test extends AbstractDistributedBucketTest<String> {

    private static GenericContainer container;
    private static JedisPool jedisPool;

    @BeforeClass
    public static void setup() {
        container = startRedisContainer();
        jedisPool = createJedisClient(container);
    }

    @AfterClass
    public static void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
        if (container != null) {
            container.close();
        }
    }

    private static JedisPool createJedisClient(GenericContainer container) {
        String redisHost = container.getHost();
        Integer redisPort = container.getMappedPort(6379);

        return new JedisPool(redisHost, redisPort);
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:7.0.2").withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    @Override
    protected ProxyManager<String> getProxyManager() {
        return JedisBasedProxyManager.builderFor(jedisPool)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.none())
                .withKeyMapper(Mapper.STRING)
                .build();
    }

    @Override
    protected String generateRandomKey() {
        return UUID.randomUUID().toString();
    }
}