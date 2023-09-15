package io.github.bucket4j.redis.jedis.cas;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JedisBasedProxyManager_ClusterMode_Test extends AbstractDistributedBucketTest<byte[]> {

    private static final Logger logger = LoggerFactory.getLogger(JedisBasedProxyManager_ClusterMode_Test.class);

    private static GenericContainer container;
    private static JedisCluster jedisCluster;

    private static final Integer[] CONTAINER_CLUSTER_PORTS = {7000, 7001, 7002, 7003, 7004, 7005};

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();
        jedisCluster = createJedisCluster(container);
    }

    @AfterAll
    public static void shutdown() {
        if (jedisCluster != null) {
            jedisCluster.close();
        }
        if (container != null) {
            container.close();
        }
    }

    private static JedisCluster createJedisCluster(GenericContainer container) {
        List<HostAndPort> shards = new ArrayList<>();
        for (Integer containerClusterPort : CONTAINER_CLUSTER_PORTS) {
            String redisHost = container.getHost();
            Integer redisPort = container.getMappedPort(containerClusterPort);
            shards.add(new HostAndPort(redisHost, redisPort));
        }

        return new JedisCluster(new HashSet<>(shards));
    }

    private static GenericContainer startRedisContainer() {
        // see this doc https://github.com/Grokzen/docker-redis-cluster
        GenericContainer genericContainer = new GenericContainer("grokzen/redis-cluster:6.0.7");

        genericContainer.withExposedPorts(CONTAINER_CLUSTER_PORTS);
        // start does not wait cluster availability
        genericContainer.start();

        List<HostAndPort> shards = new ArrayList<>();
        for (Integer containerClusterPort : CONTAINER_CLUSTER_PORTS) {
            String redisHost = genericContainer.getHost();
            Integer redisPort = genericContainer.getMappedPort(containerClusterPort);
            shards.add(new HostAndPort(redisHost, redisPort));
        }

        // need to wait until all nodes join to cluster
        for (int i = 0; i < 200; i++) {
            JedisCluster jedisClusterProbe = null;
            try {
                jedisClusterProbe = new JedisCluster(new HashSet<>(shards));
                jedisClusterProbe.get("42".getBytes());
                return genericContainer;
            } catch (Throwable e) {
                logger.error("Failed to check Redis Cluster availability: {}", e.getMessage());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } finally {
                if (jedisClusterProbe != null) {
                    jedisClusterProbe.close();
                }
            }
        }

        throw new IllegalStateException("Cluster was not assembled in " + TimeUnit.MILLISECONDS.toSeconds(200 * 100) + " seconds");
    }

    @Override
    protected ProxyManager<byte[]> getProxyManager() {
        return JedisBasedProxyManager.builderFor(jedisCluster)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofSeconds(10)))
                .build();
    }

    @Override
    protected byte[] generateRandomKey() {
        return UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    }
}