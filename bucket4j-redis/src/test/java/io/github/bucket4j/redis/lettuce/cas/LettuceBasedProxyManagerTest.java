package io.github.bucket4j.redis.lettuce.cas;

import io.github.bucket4j.distributed.ExpirationStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.lettuce.core.RedisClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class LettuceBasedProxyManagerTest extends AbstractDistributedBucketTest<byte[]> {

    private static GenericContainer container;
    private static RedisClient redisClient;

    @BeforeClass
    public static void setup() {
        container = startRedisContainer();
        redisClient = createLettuceClient(container);
    }

    @AfterClass
    public static void shutdown() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
        if (container != null) {
            container.close();
        }
    }

    private static RedisClient createLettuceClient(GenericContainer container) {
        String redisHost = container.getHost();
        Integer redisPort = container.getMappedPort(6379);
        String redisUrl = "redis://" + redisHost + ":" + redisPort;

        return RedisClient.create(redisUrl);
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:7.0.2").withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    @Override
    protected ProxyManager<byte[]> getProxyManager() {
        return new LettuceBasedProxyManager(redisClient, ClientSideConfig.getDefault(), ExpirationStrategy.none());
    }

    @Override
    protected byte[] generateRandomKey() {
        return UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    }

}