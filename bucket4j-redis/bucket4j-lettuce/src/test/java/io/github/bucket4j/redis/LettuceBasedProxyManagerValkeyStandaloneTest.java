package io.github.bucket4j.redis;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.distributed.proxy.RetryDecision;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

/**
 * Runs the same TCK suite as {@link LettuceBasedProxyManagerStandaloneTest} but against a
 * real Valkey server instead of Redis, to verify the claim in
 * <a href="https://github.com/bucket4j/bucket4j/issues/520">#520</a> that Bucket4j's Lettuce
 * integration (a general-purpose Redis protocol client, not the dedicated Glide/Valkey
 * client) is also Valkey-compatible.
 */
public class LettuceBasedProxyManagerValkeyStandaloneTest extends AbstractDistributedBucketTest {

    private static GenericContainer container;

    // lettuce
    private static RedisClient redisClient;

    @BeforeAll
    public static void setup() {
        container = startValkeyContainer();

        // lettuce
        redisClient = createLettuceClient(container);

        specs = Arrays.asList(
            // Letucce
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_ByteArrayKey",
                () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                () -> Bucket4jLettuce.casBasedBuilder(redisClient)
            ).checkExpiration(),
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jLettuce.casBasedBuilder(redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)))
            ).checkExpiration(),
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_StringKey_WithBackoffRetryStrategy",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jLettuce.casBasedBuilder(redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)))
                    .retryStrategy(metadata -> RetryDecision.retryAfter(Duration.ofNanos(metadata.getAttemptNumber())))
            ).checkExpiration()
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

    private static GenericContainer startValkeyContainer() {
        GenericContainer genericContainer = new GenericContainer("valkey/valkey:8.0")
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
