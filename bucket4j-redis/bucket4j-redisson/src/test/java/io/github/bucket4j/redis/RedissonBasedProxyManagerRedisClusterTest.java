package io.github.bucket4j.redis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.redisson.api.NatMapper;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.command.CommandAsyncService;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.ConfigSupport;
import org.redisson.connection.ConnectionManager;
import org.redisson.liveobject.core.RedissonObjectBuilder;
import org.redisson.misc.RedisURI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import io.netty.util.internal.ThreadLocalRandom;

public class RedissonBasedProxyManagerRedisClusterTest extends AbstractDistributedBucketTest {

    private static final Logger logger = LoggerFactory.getLogger(RedissonBasedProxyManagerRedisClusterTest.class);
    private static final Integer[] CONTAINER_CLUSTER_PORTS = {7000, 7001, 7002, 7003, 7004, 7005};

    private static GenericContainer container;

    private static ConnectionManager connectionManager;
    private static CommandAsyncExecutor commandExecutor;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();

        // Redisson
        connectionManager = createRedissonClient(container);
        commandExecutor = createRedissonExecutor(connectionManager);

        specs = Arrays.asList(
            // Redisson
            new ProxyManagerSpec<>(
                "RedissonBasedProxyManager_LongKey",
                () -> ThreadLocalRandom.current().nextLong(),
                () -> Bucket4jRedisson.casBasedBuilder(commandExecutor).keyMapper(Mapper.LONG)
            ).checkExpiration(),
            new ProxyManagerSpec<>(
                "RedissonBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jRedisson.casBasedBuilder(commandExecutor)
            ).checkExpiration()
        );
    }

    @AfterAll
    public static void shutdown() {
        try {
            if (connectionManager != null) {
                connectionManager.shutdown();
            }
        } finally {
            if (container != null) {
                container.close();
            }
        }
    }

    private static ConnectionManager createRedissonClient(GenericContainer container) {
        Config config = new Config();
        ClusterServersConfig clusterConfig = config.useClusterServers();
        for (Integer containerClusterPort : CONTAINER_CLUSTER_PORTS) {
            String redisHost = container.getHost();
            Integer redisPort = container.getMappedPort(containerClusterPort);
            String redisUrl = "redis://" + redisHost + ":" + redisPort;
            clusterConfig.addNodeAddress(redisUrl);
        }

        // Redis Cluster nodes advertise their internal container IP address, which is not
        // reachable from the host on many Docker setups (Docker Desktop/OrbStack NAT, VPN
        // split-tunnel routes, etc). This mapper remaps every advertised container port to
        // the host-mapped port, so Redisson only ever needs to reach container.getHost().
        clusterConfig.setNatMapper(createRedissonNatMapper(container));

        return ConfigSupport.createConnectionManager(config);
    }

    private static CommandAsyncExecutor createRedissonExecutor(ConnectionManager connectionManager) {
        return new CommandAsyncService(connectionManager, null, RedissonObjectBuilder.ReferenceType.DEFAULT);
    }

    private static Map<Integer, Integer> createContainerPortToMappedPort(GenericContainer container) {
        Map<Integer, Integer> containerPortToMappedPort = new HashMap<>();
        for (Integer port : CONTAINER_CLUSTER_PORTS) {
            containerPortToMappedPort.put(port, container.getMappedPort(port));
        }
        return containerPortToMappedPort;
    }

    private static NatMapper createRedissonNatMapper(GenericContainer container) {
        Map<Integer, Integer> containerPortToMappedPort = createContainerPortToMappedPort(container);
        return redisURI -> {
            Integer mappedPort = containerPortToMappedPort.get(redisURI.getPort());
            return mappedPort == null ? redisURI : new RedisURI(redisURI.getScheme(), container.getHost(), mappedPort);
        };
    }

    private static ClientResources createClientResources(GenericContainer container) {
        Map<Integer, Integer> containerPortToMappedPort = createContainerPortToMappedPort(container);
        Function<io.lettuce.core.internal.HostAndPort, io.lettuce.core.internal.HostAndPort> mapping = hostAndPort -> {
            Integer mappedPort = containerPortToMappedPort.get(hostAndPort.getPort());
            return mappedPort == null ? hostAndPort : io.lettuce.core.internal.HostAndPort.of(container.getHost(), mappedPort);
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
            List<io.lettuce.core.RedisURI> clusterHosts = Arrays.stream(CONTAINER_CLUSTER_PORTS)
                .map(port -> io.lettuce.core.RedisURI.create(genericContainer.getHost(), genericContainer.getMappedPort(port)))
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
