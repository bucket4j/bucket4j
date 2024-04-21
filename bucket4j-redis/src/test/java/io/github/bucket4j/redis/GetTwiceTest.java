package io.github.bucket4j.redis;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetTwiceTest {

    private static GenericContainer container;

    // lettuce
    private static RedisClient redisClient;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();

        // lettuce
        redisClient = createLettuceClient(container);
    }

    @AfterAll
    public static void shutdown() {
        try {
            if (redisClient != null) {
                redisClient.shutdown();
            }
        } finally {
            if (container != null) {
                container.close();
            }
        }
    }

    @Test
    public void testGetTwice() {
        String id = UUID.randomUUID().toString();
        ProxyManager<String> proxyManager = LettuceBasedProxyManager.builderFor(redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)))
            .withExpirationStrategy(ExpirationAfterWriteStrategy.none())
            .build();

        BucketConfiguration config = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(100).refillGreedy(10, Duration.ofSeconds(60)))
            .build();
        BucketProxy bucket = proxyManager.builder()
                .build(id, () -> config);

        // this leads to 2 calls, first call is wasted to detect that bucket does not exist,
        // so it needs to ask configSupplier to provide configuration and repeat compare&swap
        assertTrue(bucket.tryConsume(1));

        // this leads to 1 call
        assertTrue(bucket.tryConsume(1));

        // this leads to 1 calls
        assertTrue(bucket.tryConsume(1));

        // this leads to 1 calls
        assertTrue(bucket.tryConsume(1));
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:4.0.11")
            .withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    private static RedisClient createLettuceClient(GenericContainer container) {
        String redisHost = container.getHost();
        Integer redisPort = container.getMappedPort(6379);
        String redisUrl = "redis://" + redisHost + ":" + redisPort;

        return RedisClient.create(redisUrl);
    }

}
