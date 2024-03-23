package io.github.bucket4j.redis;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

/**
 * @author Vladimir Bukhtoyarov
 */
public class LettuceBasedProxyManagerStandaloneTest extends AbstractDistributedBucketTest {

    private static GenericContainer container;

    // lettuce
    private static RedisClient redisClient;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();

        // lettuce
        redisClient = createLettuceClient(container);

        specs = Arrays.asList(
            // Letucce
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_ByteArrayKey",
                () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                clientConfig -> LettuceBasedProxyManager.builderFor(redisClient)
                    .withClientSideConfig(clientConfig)
                    .build()
            ),
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                clientConfig -> LettuceBasedProxyManager.builderFor(redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)))
                    .withClientSideConfig(clientConfig)
                    .build()
            ),
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_NoExpiration_StringKey_RequestTimeout",
                () -> UUID.randomUUID().toString(),
                clientConfig -> LettuceBasedProxyManager.builderFor(redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)))
                    .withClientSideConfig(clientConfig.withRequestTimeout(Duration.ofSeconds(3)))
                    .build()
            )
        );
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
