package io.github.bucket4j.redis;

import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.vertx.Bucket4jVertx;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class VertxBasedProxyManagerStandaloneTest extends AbstractDistributedBucketTest {

    private static GenericContainer container;
    private static Vertx vertx;
    private static Redis redis;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();
        vertx = Vertx.vertx();
        redis = createVertxClient(container, vertx);

        BackwardCompatibilityStateCheckHelper<byte[]> byteArrayKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(byte[] key) {
                Response response = redis.send(Request.cmd(Command.GET).arg(key))
                    .toCompletionStage().toCompletableFuture().join();
                return response == null ? null : response.toBytes();
            }

            @Override
            public void setRawState(byte[] key, byte[] state) {
                redis.send(Request.cmd(Command.SET).arg(key).arg(state))
                    .toCompletionStage().toCompletableFuture().join();
            }
        };

        BackwardCompatibilityStateCheckHelper<String> stringKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                return byteArrayKeyHelper.getRawState(key.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void setRawState(String key, byte[] state) {
                byteArrayKeyHelper.setRawState(key.getBytes(StandardCharsets.UTF_8), state);
            }
        };

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "VertxBasedProxyManager_ByteArrayKey",
                () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                () -> Bucket4jVertx.casBasedBuilder(redis)
            ).checkExpiration().checkStateBackwardCompatibility(byteArrayKeyHelper).withoutBackwardCompatibilityRequestChecker(),
            new ProxyManagerSpec<>(
                "VertxBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jVertx.casBasedBuilder(redis).keyMapper(Mapper.STRING)
            ).checkExpiration().checkStateBackwardCompatibility(stringKeyHelper).withoutBackwardCompatibilityRequestChecker()
        );
    }

    @AfterAll
    public static void shutdown() {
        try {
            try {
                if (redis != null) {
                    redis.close().toCompletionStage().toCompletableFuture().join();
                }
            } finally {
                if (vertx != null) {
                    vertx.close().toCompletionStage().toCompletableFuture().join();
                }
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

    private static Redis createVertxClient(GenericContainer container, Vertx vertx) {
        String redisUrl = "redis://" + container.getHost() + ":" + container.getMappedPort(6379);
        RedisOptions redisOptions = new RedisOptions().setConnectionString(redisUrl);
        return Redis.createClient(vertx, redisOptions);
    }

}
