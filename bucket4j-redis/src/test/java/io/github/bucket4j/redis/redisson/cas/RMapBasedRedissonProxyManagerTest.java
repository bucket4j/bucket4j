package io.github.bucket4j.redis.redisson.cas;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

public class RMapBasedRedissonProxyManagerTest extends AbstractDistributedBucketTest<String> {

    private static String MAP_NAME = "buckets";

    private static GenericContainer container;
    private static RedissonClient redisson;
    private static RMap<String, byte[]> buckets;

    @BeforeClass
    public static void setup() {
        container = startRedisContainer();
        redisson = createRedissonClient(container);
        buckets = redisson.getMap(MAP_NAME);
    }

    @AfterClass
    public static void shutdown() {
        if (redisson != null) {
            redisson.shutdown();
        }
        if (container != null) {
            container.close();
        }
    }

    private static RedissonClient createRedissonClient(GenericContainer container) {
        String redisAddress = container.getContainerIpAddress();
        Integer redisPort = container.getMappedPort(6379);
        String redisUrl = "redis://" + redisAddress + ":" + redisPort;

        Config config = new Config();
        config.useSingleServer().setAddress(redisUrl);

        return Redisson.create(config);
    }

    private static GenericContainer startRedisContainer() {
        GenericContainer genericContainer = new GenericContainer("redis:4.0.11")
                .withExposedPorts(6379);
        genericContainer.start();
        return genericContainer;
    }

    @Override
    protected ProxyManager<String> getProxyManager() {
        return new RMapBasedRedissonProxyManager<>(buckets, ClientSideConfig.getDefault());
    }

    @Override
    protected String generateRandomKey() {
        return UUID.randomUUID().toString();
    }

}