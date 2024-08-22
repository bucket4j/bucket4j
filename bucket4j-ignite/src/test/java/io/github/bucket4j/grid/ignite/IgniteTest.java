package io.github.bucket4j.grid.ignite;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executors;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.AsyncProxyManagerSpec;
import io.github.bucket4j.tck.ProxyManagerSpec;

import static io.github.bucket4j.distributed.proxy.ExecutionStrategy.background;
import static io.github.bucket4j.distributed.proxy.ExecutionStrategy.backgroundTimeBounded;

public class IgniteTest extends AbstractDistributedBucketTest {

    private static IgniteCache<String, byte[]> cache;
    private static Cloud cloud;
    private static ViNode server;

    private static Ignite ignite;

    @BeforeAll
    public static void setup() throws UnknownHostException {
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

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "IgniteProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite.thickClient().entryProcessorBasedBuilder(cache)
            ),
            new ProxyManagerSpec<>(
                "IgniteProxyManager_background",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite.thickClient()
                    .entryProcessorBasedBuilder(cache)
                    .executionStrategy(background(Executors.newFixedThreadPool(20)))
            ),
            new ProxyManagerSpec<>(
                "IgniteProxyManager_backgroundTimeBounded",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite.thickClient()
                    .entryProcessorBasedBuilder(cache)
                    .executionStrategy(backgroundTimeBounded(Executors.newFixedThreadPool(20), Duration.ofSeconds(5)))
            )
        );

        asyncSpecs = Arrays.asList(
            new AsyncProxyManagerSpec<>(
                "IgniteAsyncProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite.thickClient().asyncEntryProcessorBasedBuilder(cache)
            )
        );
    }


    @AfterAll
    public static void shutdown() {
        if (ignite != null) {
            ignite.close();
        }
        if (cloud != null) {
            cloud.shutdown();
        }
    }

}
