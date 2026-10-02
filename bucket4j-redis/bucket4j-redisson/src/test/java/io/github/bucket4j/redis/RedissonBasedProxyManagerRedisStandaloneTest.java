package io.github.bucket4j.redis;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.command.CommandAsyncService;
import org.redisson.config.Config;
import org.redisson.config.ConfigSupport;
import org.redisson.connection.ConnectionManager;
import org.redisson.liveobject.core.RedissonObjectBuilder;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;
import io.lettuce.core.RedisClient;
import io.netty.util.internal.ThreadLocalRandom;

public class RedissonBasedProxyManagerRedisStandaloneTest extends AbstractDistributedBucketTest {

    private static GenericContainer container;

    // redisson
    private static ConnectionManager connectionManager;
    private static CommandAsyncExecutor commandExecutor;

    // lettuce
    private static RedisClient redisClient;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();

        // Redisson
        connectionManager = createRedissonClient(container);
        commandExecutor = createRedissonExecutor(connectionManager);

        // lettuce
        redisClient = createLettuceClient(container);

        BackwardCompatibilityStateCheckHelper<Long> longKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(Long key) {
                String stringKey = Mapper.LONG.toString(key);
                return commandExecutor.get(commandExecutor.readAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.GET, stringKey));
            }

            @Override
            public void setRawState(Long key, byte[] state) {
                String stringKey = Mapper.LONG.toString(key);
                commandExecutor.get(commandExecutor.writeAsync(stringKey, ByteArrayCodec.INSTANCE, RedisCommands.SET, stringKey, state));
            }
        };

        BackwardCompatibilityStateCheckHelper<String> stringKeyHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                return commandExecutor.get(commandExecutor.readAsync(key, ByteArrayCodec.INSTANCE, RedisCommands.GET, key));
            }

            @Override
            public void setRawState(String key, byte[] state) {
                commandExecutor.get(commandExecutor.writeAsync(key, ByteArrayCodec.INSTANCE, RedisCommands.SET, key, state));
            }
        };

        specs = Arrays.asList(
            // Redisson
            new ProxyManagerSpec<>(
                "RedissonBasedProxyManager_LongKey",
                () -> ThreadLocalRandom.current().nextLong(),
                () -> Bucket4jRedisson.casBasedBuilder(commandExecutor).keyMapper(Mapper.LONG)
            ).checkExpiration().checkStateBackwardCompatibility(longKeyHelper).withoutBackwardCompatibilityRequestChecker(),
            new ProxyManagerSpec<>(
                "RedissonBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jRedisson.casBasedBuilder(commandExecutor)
            ).checkExpiration().checkStateBackwardCompatibility(stringKeyHelper).withoutBackwardCompatibilityRequestChecker()
        );
    }

    @AfterAll
    public static void shutdown() {
        try {
            try {
                if (connectionManager != null) {
                    connectionManager.shutdown();
                }
            } finally {
                if (redisClient != null) {
                    redisClient.shutdown();
                }
            }
        } finally {
            if (container != null) {
                container.close();
            }
        }
    }

    private static ConnectionManager createRedissonClient(GenericContainer container) {
        String redisAddress = container.getContainerIpAddress();
        Integer redisPort = container.getMappedPort(6379);
        String redisUrl = "redis://" + redisAddress + ":" + redisPort;

        Config config = new Config();
        config.useSingleServer().setAddress(redisUrl);

        ConnectionManager connectionManager = ConfigSupport.createConnectionManager(config);
        return connectionManager;
    }

    private static CommandAsyncExecutor createRedissonExecutor(ConnectionManager connectionManager) {
        return new CommandAsyncService(connectionManager, null, RedissonObjectBuilder.ReferenceType.DEFAULT);
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
