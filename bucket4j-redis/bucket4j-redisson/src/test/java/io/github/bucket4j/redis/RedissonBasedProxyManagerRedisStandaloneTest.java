package io.github.bucket4j.redis;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.command.CommandAsyncService;
import org.redisson.config.Config;
import org.redisson.config.ConfigSupport;
import org.redisson.connection.ConnectionManager;
import org.redisson.liveobject.core.RedissonObjectBuilder;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import io.lettuce.core.RedisClient;
import io.netty.util.internal.ThreadLocalRandom;

/**
 * @author Vladimir Bukhtoyarov
 */
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

        specs = Arrays.asList(
            // Redisson
            new ProxyManagerSpec<>(
                "RedissonBasedProxyManager_LongKey",
                () -> ThreadLocalRandom.current().nextLong(),
                clientConfig -> RedissonBasedProxyManager.builderFor(commandExecutor)
                    .withKeyMapper(Mapper.LONG)
                    .withClientSideConfig(clientConfig)
                    .build()
            ),
            new ProxyManagerSpec<>(
                "RedissonBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                clientConfig -> RedissonBasedProxyManager.builderFor(commandExecutor)
                    .withClientSideConfig(clientConfig)
                    .build()
            ),
            new ProxyManagerSpec<>(
                "RedissonBasedProxyManager_StringKey_RequestTimeout",
                () -> UUID.randomUUID().toString(),
                clientConfig -> RedissonBasedProxyManager.builderFor(commandExecutor)
                    .withClientSideConfig(clientConfig.withRequestTimeout(Duration.ofSeconds(3)))
                    .build()
            )
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
