package io.github.bucket4j.redis.redisson.cas;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.redisson.command.CommandExecutor;
import org.redisson.command.CommandSyncService;
import org.redisson.config.Config;
import org.redisson.config.ConfigSupport;
import org.redisson.connection.ConnectionManager;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class RedissonBasedProxyManagerTest extends AbstractDistributedBucketTest<String> {

    private static GenericContainer container;
    private static ConnectionManager connectionManager;
    private static CommandExecutor commandExecutor;

    @BeforeClass
    public static void setup() {
        container = startRedisContainer();
        connectionManager = createRedissonClient(container);
        commandExecutor = createRedissonExecutor(connectionManager);
    }

    @AfterClass
    public static void shutdown() {
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
        if (container != null) {
            container.close();
        }
    }

    @Test
    public void test_Issue_279() {
        RedissonBasedProxyManager proxyManager = RedissonBasedProxyManager.builderFor(commandExecutor)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.none())
                .build();

        int MIN_CAPACITY = 4;
        int MAX_CAPACITY = 10;
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(MIN_CAPACITY, Refill.intervally(4, Duration.ofMinutes(20))))
                .addLimit(Bandwidth.classic(MAX_CAPACITY, Refill.intervally(10, Duration.ofMinutes(60))))
                .build();

        Bucket bucket = proxyManager.builder().build("something", configuration);

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

    private static ConnectionManager createRedissonClient(GenericContainer container) {
        String redisAddress = container.getContainerIpAddress();
        Integer redisPort = container.getMappedPort(6379);
        String redisUrl = "redis://" + redisAddress + ":" + redisPort;

        Config config = new Config();
        config.useSingleServer().setAddress(redisUrl);

        ConnectionManager connectionManager = ConfigSupport.createConnectionManager(config);
        return connectionManager;
    }

    private static CommandExecutor createRedissonExecutor(ConnectionManager connectionManager) {
        return new CommandSyncService(connectionManager, null);
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