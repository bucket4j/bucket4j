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
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

public class LettuceBasedProxyManagerStandaloneTest extends AbstractDistributedBucketTest {

    private static GenericContainer container;

    // lettuce
    private static RedisClient redisClient;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();

        // lettuce
        redisClient = createLettuceClient(container);

        StatefulRedisConnection<byte[], byte[]> byteArrayRawConnection = redisClient.connect(ByteArrayCodec.INSTANCE);
        BackwardCompatibilityStateCheckHelper<byte[]> byteArrayKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(byte[] key) {
                return byteArrayRawConnection.sync().get(key);
            }

            @Override
            public void setRawState(byte[] key, byte[] state) {
                byteArrayRawConnection.sync().set(key, state);
            }
        };

        StatefulRedisConnection<String, byte[]> stringRawConnection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        BackwardCompatibilityStateCheckHelper<String> stringKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                return stringRawConnection.sync().get(key);
            }

            @Override
            public void setRawState(String key, byte[] state) {
                stringRawConnection.sync().set(key, state);
            }
        };

        specs = Arrays.asList(
            // Letucce
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_ByteArrayKey",
                () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                () -> Bucket4jLettuce.casBasedBuilder(redisClient)
            ).checkExpiration().checkStateBackwardCompatibility(byteArrayKeyHelper),
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jLettuce.casBasedBuilder(redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)))
            ).checkExpiration().checkStateBackwardCompatibility(stringKeyHelper),
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_StringKey_WithBackoffRetryStrategy",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jLettuce.casBasedBuilder(redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)))
                    .retryStrategy(metadata -> RetryDecision.retryAfter(Duration.ofNanos(metadata.getAttemptNumber())))
            ).checkExpiration().checkStateBackwardCompatibility(stringKeyHelper)
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
