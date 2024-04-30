package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.*;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;


public class IgniteThinClientTest extends AbstractDistributedBucketTest {

    private static final String CACHE_NAME = "my_buckets";
    public static final String CACHE2_NAME = CACHE_NAME + "_byte_buffer";

    private static ClientCache<String, byte[]> cache;
    private static ClientCache<String, ByteBuffer> cache2;
    private static Cloud cloud;
    private static ViNode server;

    private static IgniteClient igniteClient;

    @BeforeAll
    public static void setup() {
        // start separated JVM on current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        ADD_OPENS.forEach(arg -> cloud.node("**").x(VX.JVM).addJvmArg(arg));
        server = cloud.node("stateful-ignite-server");

        int serverDiscoveryPort = 47500;
        //        String serverNodeAdress = InetAddress.getLocalHost().getHostAddress() + ":" + serverDiscoveryPort;
        String serverNodeAdress = "localhost:" + serverDiscoveryPort;

        server.exec((Runnable & Serializable) () -> {
            TcpDiscoveryVmIpFinder neverFindOthers = new TcpDiscoveryVmIpFinder();
            neverFindOthers.setAddresses(Collections.singleton(serverNodeAdress));

            TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();
            tcpDiscoverySpi.setIpFinder(neverFindOthers);
            tcpDiscoverySpi.setLocalPort(serverDiscoveryPort);

            ThinClientConfiguration thinClientCfg = new ThinClientConfiguration()
                    .setMaxActiveComputeTasksPerConnection(100);
            ClientConnectorConfiguration clientConnectorCfg = new ClientConnectorConfiguration()
                    .setThinClientConfiguration(thinClientCfg);

            IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
            igniteConfiguration.setClientMode(false);
            igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
            igniteConfiguration.setClientConnectorConfiguration(clientConnectorCfg);

            Ignite ignite = Ignition.start(igniteConfiguration);

            ignite.getOrCreateCache(new CacheConfiguration(CACHE_NAME));
            ignite.getOrCreateCache(new CacheConfiguration(CACHE2_NAME));
        });

        // start ignite thin client which works inside current JVM and does not hold data
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setAddresses("localhost:" + ClientConnectorConfiguration.DFLT_PORT);

        igniteClient = Ignition.startClient(clientConfiguration);

        cache = igniteClient.cache(CACHE_NAME);
        cache2 = igniteClient.cache(CACHE2_NAME);

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "IgniteThinClientCompute",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite.thinClient().clientComputeBasedBuilder(cache, igniteClient.compute())
            ),
            new ProxyManagerSpec<>(
                "IgniteThinClientCas",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite.thinClient().casBasedBuilder(cache2)
            )
        );
    }

    @AfterAll
    public static void shutdown() throws Exception {
        if (igniteClient != null) {
            igniteClient.close();
        }
        if (cloud != null) {
            cloud.shutdown();
        }
    }

}
