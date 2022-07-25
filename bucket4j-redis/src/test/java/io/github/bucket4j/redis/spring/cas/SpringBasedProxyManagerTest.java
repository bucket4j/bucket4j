package io.github.bucket4j.redis.spring.cas;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.lettuce.core.RedisClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

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

    @Test
    public void test_Issue_279() {
        SpringDataRedisBasedProxyManager proxyManager = SpringDataRedisBasedProxyManager.builderFor(redisCommands)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.none())
                .build();

        int MIN_CAPACITY = 4;
        int MAX_CAPACITY = 10;
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(MIN_CAPACITY, Refill.intervally(4, Duration.ofMinutes(20))))
                .addLimit(Bandwidth.classic(MAX_CAPACITY, Refill.intervally(10, Duration.ofMinutes(60))))
                .build();

        byte[] key = "something".getBytes(StandardCharsets.UTF_8);
        Bucket bucket = proxyManager.builder().build(key, configuration);

        for (int i = 1; i <= 4; i++) {
            VerboseResult<ConsumptionProbe> verboseResult = bucket.asVerbose().tryConsumeAndReturnRemaining(1);
            ConsumptionProbe probe = verboseResult.getValue();
            long[] availableTokensPerEachBandwidth = verboseResult.getDiagnostics().getAvailableTokensPerEachBandwidth();
            System.out.println("Remaining tokens = " + probe.getRemainingTokens());
            System.out.println("Tokens per bandwidth = " + Arrays.toString(availableTokensPerEachBandwidth));
            assertEquals(MIN_CAPACITY - i, probe.getRemainingTokens());
            assertEquals(MIN_CAPACITY - i, availableTokensPerEachBandwidth[0]);
            assertEquals(MAX_CAPACITY - i, availableTokensPerEachBandwidth[1]);
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
        return SpringDataRedisBasedProxyManager.builderFor(redisCommands)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.none())
                .build();
    }

    @Override
    protected byte[] generateRandomKey() {
        return UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    }
}