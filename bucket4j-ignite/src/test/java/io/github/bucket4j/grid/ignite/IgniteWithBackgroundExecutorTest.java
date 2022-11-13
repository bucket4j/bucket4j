package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ExecutionStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.ignite.thick.IgniteProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
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
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executors;

public class IgniteWithBackgroundExecutorTest extends AbstractDistributedBucketTest<String> {

    private static IgniteCache<String, byte[]> cache;
    private static Cloud cloud;
    private static ViNode server;

    private static Ignite ignite;

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

            IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
            igniteConfiguration.setClientMode(false);
            igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);

            CacheConfiguration cacheConfiguration = new CacheConfiguration("my_buckets");
            Ignite ignite = Ignition.start(igniteConfiguration);
            ignite.getOrCreateCache(cacheConfiguration);
        });

        // start ignite client which works inside current JVM and does not hold data
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Collections.singleton(serverNodeAdress));
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();
        tcpDiscoverySpi.setIpFinder(ipFinder);

        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
        igniteConfiguration.setClientMode(true);
        ignite = Ignition.start(igniteConfiguration);
        CacheConfiguration cacheConfiguration = new CacheConfiguration("my_buckets");
        cache = ignite.getOrCreateCache(cacheConfiguration);
    }


    @AfterClass
    public static void shutdown() {
        if (ignite != null) {
            ignite.close();
        }
        if (cloud != null) {
            cloud.shutdown();
        }
    }

    @Override
    protected ProxyManager<String> getProxyManager() {
        return new IgniteProxyManager<>(cache,
                ClientSideConfig.getDefault().withExecutionStrategy(ExecutionStrategy.background(Executors.newFixedThreadPool(20)))
        );
    }

    @Override
    protected String generateRandomKey() {
        return UUID.randomUUID().toString();
    }

}
