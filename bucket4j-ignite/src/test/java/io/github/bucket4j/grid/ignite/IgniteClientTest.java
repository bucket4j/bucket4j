package io.github.bucket4j.grid.ignite;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Collections;
import io.github.bucket4j.AbstractDistributedBucketTest;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.RecoveryStrategy;
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
import org.junit.Test;

public class IgniteClientTest extends AbstractDistributedBucketTest<IgniteBucketBuilder, io.github.bucket4j.grid.ignite.Ignite> {

    private static final String CACHE_NAME = "my_buckets";

    private static ClientCache<String, GridBucketState> cache;
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

    @Test(expected = IllegalArgumentException.class)
    @Override
    public void testThatImpossibleToPassNullCacheToProxyManagerConstructor() {
        Bucket4j.extension(getExtensionClass()).proxyManagerForCache(igniteClient.compute(), (ClientCache<String, GridBucketState>)null);
    }

    @Override
    protected Class<io.github.bucket4j.grid.ignite.Ignite> getExtensionClass() {
        return io.github.bucket4j.grid.ignite.Ignite.class;
    }

    @Override
    protected Bucket build(IgniteBucketBuilder builder, String key, RecoveryStrategy recoveryStrategy) {
        return builder.build(igniteClient.compute(), cache, key, recoveryStrategy);
    }

    @Override
    protected ProxyManager<String> newProxyManager() {
        return Bucket4j.extension(getExtensionClass()).proxyManagerForCache(igniteClient.compute(), cache);
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        cache.remove(key);
    }

}
