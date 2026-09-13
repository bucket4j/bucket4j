package io.github.bucket4j.redis;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.MappingSocketAddressResolver;

public class LettuceBasedProxyManagerClusterTest extends AbstractDistributedBucketTest {

    private static final Logger logger = LoggerFactory.getLogger(LettuceBasedProxyManagerClusterTest.class);
    private static final Integer[] CONTAINER_CLUSTER_PORTS = {7000, 7001, 7002, 7003, 7004, 7005};

    private static GenericContainer container;

    private static ClientResources clientResources;

    private static RedisClusterClient redisClient;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();

        // Redis Cluster nodes advertise their internal container IP address, which is not
        // reachable from the host on many Docker setups (Docker Desktop/OrbStack NAT, VPN
        // split-tunnel routes, etc). This resolver remaps every advertised container port to
        // the host-mapped port, so Lettuce only ever needs to reach container.getHost().
        clientResources = createClientResources(container);

        // Lettuce
        redisClient = createLettuceClient(container, clientResources);

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "LettuceBasedProxyManager_ByteArrayKey",
                () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                () -> Bucket4jLettuce.casBasedBuilder(redisClient)
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
            try {
                if (clientResources != null) {
                    clientResources.shutdown();
                }
            } finally {
                if (container != null) {
                    container.close();
                }
            }
        }
    }

    private static RedisClusterClient createLettuceClient(GenericContainer container, ClientResources clientResources) {
        List<RedisURI> clusterHosts = Arrays.stream(CONTAINER_CLUSTER_PORTS)
            .map(port -> RedisURI.create(container.getHost(), container.getMappedPort(port)))
            .collect(Collectors.toList());

        return RedisClusterClient.create(clientResources, clusterHosts);
    }

    private static ClientResources createClientResources(GenericContainer container) {
        Map<Integer, Integer> containerPortToMappedPort = new HashMap<>();
        for (Integer port : CONTAINER_CLUSTER_PORTS) {
            containerPortToMappedPort.put(port, container.getMappedPort(port));
        }

        Function<HostAndPort, HostAndPort> mapping = hostAndPort -> {
            Integer mappedPort = containerPortToMappedPort.get(hostAndPort.getPort());
            return mappedPort == null ? hostAndPort : HostAndPort.of(container.getHost(), mappedPort);
        };

        return ClientResources.builder()
            .socketAddressResolver(MappingSocketAddressResolver.create(mapping))
            .build();
    }

    private static GenericContainer startRedisContainer() {
        // see this doc https://github.com/Grokzen/docker-redis-cluster
        GenericContainer genericContainer = new GenericContainer("grokzen/redis-cluster:6.0.7");

        genericContainer.withExposedPorts(CONTAINER_CLUSTER_PORTS);
        // start does not wait cluster availability
        genericContainer.start();

        ClientResources probeClientResources = createClientResources(genericContainer);
        try {
            List<RedisURI> clusterHosts = Arrays.stream(CONTAINER_CLUSTER_PORTS)
                .map(port -> RedisURI.create(genericContainer.getHost(), genericContainer.getMappedPort(port)))
                .collect(Collectors.toList());

            // need to wait until all nodes join to cluster
            for (int i = 0; i < 200; i++) {
                RedisClusterClient redisClientProbe = null;
                try {
                    redisClientProbe = RedisClusterClient.create(probeClientResources, clusterHosts);
                    StatefulRedisClusterConnection<String, String> clusterConnection = redisClientProbe.connect();
                    AtomicInteger availableSlots = new AtomicInteger();
                    clusterConnection.getPartitions().forEach(partition -> partition.forEachSlot(slot -> availableSlots.incrementAndGet()));
                    if (availableSlots.get() < SlotHash.SLOT_COUNT) {
                        throw new IllegalStateException("Only " + availableSlots.get() + " slots is ready from required " + SlotHash.SLOT_COUNT);
                    }

                    clusterConnection.sync().get("42");
                    return genericContainer;
                } catch (Throwable e) {
                    logger.error("Failed to check Redis Cluster availability: {}", e.getMessage(), e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                } finally {
                    if (redisClientProbe != null) {
                        redisClientProbe.shutdown();
                    }
                }
            }
        } finally {
            probeClientResources.shutdown();
        }

        throw new IllegalStateException("Cluster was not assembled in " + TimeUnit.MILLISECONDS.toSeconds(200 * 100) + " seconds");
    }

}
