package io.github.bucket4j.redis;

import glide.api.GlideClusterClient;
import glide.api.models.configuration.GlideClusterClientConfiguration;
import glide.api.models.configuration.NodeAddress;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.glide.Bucket4jGlide;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class GlideBasedProxyManagerClusterTest extends AbstractDistributedBucketTest {

    private static final Logger logger = LoggerFactory.getLogger(GlideBasedProxyManagerClusterTest.class);
    private static final Integer[] CONTAINER_CLUSTER_PORTS = {7000, 7001, 7002, 7003, 7004, 7005};

    private static GenericContainer container;

    private static GlideClusterClient client;

    @BeforeAll
    public static void setup() {
        container = startRedisContainer();
        client = createGlideClusterClient(container);

        specs = Arrays.asList(
                new ProxyManagerSpec<>(
                        "GlideBasedProxyManager_ByteArrayKey",
                        () -> UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8),
                        () -> Bucket4jGlide.casBasedBuilder(client)
                ).checkExpiration(),
                new ProxyManagerSpec<>(
                        "GlideBasedProxyManager_StringKey",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jGlide.casBasedBuilder(client).keyMapper(Mapper.STRING)
                ).checkExpiration()
        );
    }

    @AfterAll
    public static void shutdown() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            if (container != null) {
                container.close();
            }
        }
    }

    private static GlideClusterClient createGlideClusterClient(GenericContainer container) {
        List<NodeAddress> addresses = new ArrayList<>();
        for (var containerClusterPort : CONTAINER_CLUSTER_PORTS) {
            addresses.add(NodeAddress.builder()
                    .host(container.getHost())
                    .port(containerClusterPort)
                    .build());
        }
        GlideClusterClientConfiguration config = GlideClusterClientConfiguration.builder()
                .lazyConnect(true)
                .addresses(addresses)
                .build();
        try {
            return GlideClusterClient.createClient(config).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static GenericContainer startRedisContainer() {
        // Redis Cluster nodes announce the address that was used to build the cluster
        // (the IP env var below) as their own address in CLUSTER SLOTS/MOVED replies.
        // By default grokzen/redis-cluster auto-detects the container's internal bridge
        // IP, which is not reachable from the host on many Docker setups (Docker
        // Desktop/OrbStack NAT, VPN split-tunnel routes, etc). Glide's Java client has
        // no client-side NAT-mapping hook (unlike Lettuce/Jedis/Redisson), so instead we
        // force the container to announce 127.0.0.1 and bind its ports 1:1 to the host,
        // so whatever the client is told always resolves back to the same container.
        GenericContainer genericContainer = new GenericContainer("grokzen/redis-cluster:6.2.14") {
            {
                for (Integer port : CONTAINER_CLUSTER_PORTS) {
                    addFixedExposedPort(port, port);
                }
            }
        };
        genericContainer.withExposedPorts(CONTAINER_CLUSTER_PORTS);
        genericContainer.withEnv("IP", "127.0.0.1");
        genericContainer.start();

        for (int i = 0; i < 200; i++) {
            try {
                String clusterInfo = genericContainer.execInContainer("redis-cli", "-p", "7000", "cluster", "info").getStdout();
                if (clusterInfo.contains("cluster_state:ok")) {
                    return genericContainer;
                }
            } catch (Exception e) {
                logger.error("Failed to check Redis Cluster availability: {}", e.getMessage(), e);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        throw new IllegalStateException("Cluster was not assembled in 200 seconds");
    }

}
