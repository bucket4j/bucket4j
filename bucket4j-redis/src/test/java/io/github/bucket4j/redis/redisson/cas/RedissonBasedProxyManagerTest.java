package io.github.bucket4j.redis.redisson.cas;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.command.CommandAsyncService;
import org.redisson.config.Config;
import org.redisson.config.ConfigSupport;
import org.redisson.connection.ConnectionManager;
import org.redisson.liveobject.core.RedissonObjectBuilder;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

public class RedissonBasedProxyManagerTest extends AbstractDistributedBucketTest<String> {

    private static GenericContainer container;
    private static ConnectionManager connectionManager;
    private static CommandAsyncExecutor commandExecutor;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();
        connectionManager = createRedissonClient(container);
        commandExecutor = createRedissonExecutor(connectionManager);
    }

    @AfterAll
    public static void shutdown() {
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
        if (container != null) {
            container.close();
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

    @Override
    protected ProxyManager<String> getProxyManager() {
        return RedissonBasedProxyManager.builderFor(commandExecutor)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.none())
                .build();
    }

    @Override
    protected String generateRandomKey() {
        return UUID.randomUUID().toString();
    }

}
