package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.grid.ignite.thin.cas.IgniteThinClientCasBasedBackend;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;


public class IgniteCasBasedClientTest extends AbstractDistributedBucketTest {

    private static final String CACHE_NAME = "my_buckets";

    private static ClientCache<String, ByteBuffer> cache;
    private static Cloud cloud;
    private static ViNode server;

    private static IgniteClient igniteClient;

    @BeforeClass
    public static void setup() throws UnknownHostException {
        // start separated JVM on current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
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

            CacheConfiguration cacheConfiguration = new CacheConfiguration(CACHE_NAME);
            ignite.getOrCreateCache(cacheConfiguration);
        });

        // start ignite thin client which works inside current JVM and does not hold data
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setAddresses("localhost:" + ClientConnectorConfiguration.DFLT_PORT);

        igniteClient = Ignition.startClient(clientConfiguration);

        cache = igniteClient.cache(CACHE_NAME);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (igniteClient != null) {
            igniteClient.close();
        }
        if (cloud != null) {
            cloud.shutdown();
        }
    }

    @Override
    protected Backend<String> getBackend() {
        return new IgniteThinClientCasBasedBackend<>(cache, ClientSideConfig.getDefault());
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        cache.remove(key);
    }

}
