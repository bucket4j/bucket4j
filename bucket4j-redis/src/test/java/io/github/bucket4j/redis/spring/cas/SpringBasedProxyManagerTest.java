package io.github.bucket4j.redis.spring.cas;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.lettuce.core.RedisClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class SpringBasedProxyManagerTest extends AbstractDistributedBucketTest<byte[]> {

    private static GenericContainer container;
    private static RedisCommands redisCommands;

    @BeforeClass
    public static void setup() {
        container = startRedisContainer();
        redisCommands = createSpringClient(container);
    }

    @AfterClass
    public static void shutdown() {
        if (redisCommands != null) {
            redisCommands.shutdown();
        }
        if (container != null) {
            container.close();
        }
    }

    private static RedisCommands createSpringClient(GenericContainer container) {
        String redisHost = container.getHost();
        Integer redisPort = container.getMappedPort(6379);
        String redisUrl = "redis://" + redisHost + ":" + redisPort;

        return new LettuceConnection(2000, RedisClient.create(redisUrl));
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:7.0.2").withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    @Override
    protected ProxyManager<byte[]> getProxyManager() {
        return new SpringBasedProxyManager(redisCommands, ClientSideConfig.getDefault(), Duration.ofMinutes(10));
    }

    @Override
    protected byte[] generateRandomKey() {
        return UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    }
}