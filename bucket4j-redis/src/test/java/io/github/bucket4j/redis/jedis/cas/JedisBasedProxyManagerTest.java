package io.github.bucket4j.redis.jedis.cas;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class JedisBasedProxyManagerTest extends AbstractDistributedBucketTest<byte[]> {

    private static GenericContainer container;
    private static JedisPool jedisPool;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();
        jedisPool = createJedisClient(container);
    }

    @AfterAll
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
    protected ProxyManager<byte[]> getProxyManager() {
        return JedisBasedProxyManager.builderFor(jedisPool)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.none())
                .build();
    }

    @Override
    protected byte[] generateRandomKey() {
        return UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void testConfigurationReplacement() throws InterruptedException {
        ProxyManager<byte[]> jedisBasedProxyManager = getProxyManager();
        for (TokensInheritanceStrategy strategy : TokensInheritanceStrategy.values()) {
            System.out.println("=========================");
            System.out.println("Setting bandwith to 5/min");
            // Bucket bucket = Bucket.builder().addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofMinutes(1))).build();
            Bucket bucket = jedisBasedProxyManager.builder().build(strategy.name().getBytes(), () -> BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofMinutes(1))).build());
            Assertions.assertTrue(bucket.tryConsume(5));
            Assertions.assertFalse(bucket.tryConsume(1));
            System.out.println("Update configuration to 10/min using strategy " + strategy);
            BucketConfiguration newConfig = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1))).build();
            bucket.replaceConfiguration(newConfig, strategy);
            System.out.println("Sleeping 60+ seconds in order to refill all tokens");
            Thread.sleep(61000);
            Assertions.assertTrue(bucket.tryConsume(10));
            Assertions.assertFalse(bucket.tryConsume(1));
        }
    }

}