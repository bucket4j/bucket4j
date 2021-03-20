package io.github.bucket4j.grid.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.configuration.*;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.cache.processor.EntryProcessor;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class InvestigationOfThinclientApiTest {

    private static final String CACHE_NAME = "my_buckets";
    private static ClientCache<String, Long> cache;
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

    private static class TestComputeJob implements ComputeJob {

        private final TestTaskParams params;

        @IgniteInstanceResource
        private org.apache.ignite.Ignite ignite;

        private TestComputeJob(TestTaskParams params) {
            this.params = params;
        }

        @Override
        public void cancel() {

        }

        @Override
        public Object execute() throws IgniteException {
            long increment = params.increment;
            EntryProcessor<String, Long, Long> entryProcessor = (entry, arguments) -> {
                if (!entry.exists()) {
                    entry.setValue(increment);
                    return increment;
                } else {
                    long value = entry.getValue() + increment;
                    entry.setValue(value);
                    return value;
                }
            };
            IgniteCache<String, Long> cache = ignite.cache(params.cacheName);
            return cache.invoke(params.key, entryProcessor);
        }
    }

    public static class TestComputeTask extends ComputeTaskAdapter<TestTaskParams, Long> {

        @IgniteInstanceResource
        private Ignite ignite;

        @Override
        public @NotNull Map<? extends ComputeJob, ClusterNode> map(List<ClusterNode> subgrid, @Nullable TestTaskParams params) throws IgniteException {
            TestComputeJob job = new TestComputeJob(params);

            @Nullable ClusterNode primaryNodeForKey = ignite.affinity(CACHE_NAME).mapKeyToNode(params.key);
            for (ClusterNode clusterNode : subgrid) {
                if (clusterNode == primaryNodeForKey) {
                    return Collections.singletonMap(job, clusterNode);
                }
            }
            throw new IllegalStateException("Unuble to locate primary node for key " + params.key);
        }

        @Override
        public @Nullable Long reduce(List<ComputeJobResult> results) throws IgniteException {
            return results.get(0).getData();
        }
    }

    public static final class TestTaskParams {

        private final String cacheName;
        private final String key;
        private final long increment;

        public TestTaskParams(String cacheName, String key, long increment) {
            this.cacheName = cacheName;
            this.key = key;
            this.increment = increment;
        }
    }

    @Test
    public void testThinClientApi() throws InterruptedException {
        String name = TestComputeTask.class.getName();

        TestTaskParams params = new TestTaskParams(CACHE_NAME, "42", 1);
        long result = igniteClient.compute().execute(name, params);
        System.out.println(result);
        assertEquals(1l, result);
    }

    @Test
    public void testThinClientApiTransactionIsolation() throws InterruptedException {
        String name = TestComputeTask.class.getName();

        TestTaskParams params = new TestTaskParams(CACHE_NAME, "42", 1);
        String key = "42";
        int threads = 4;
        int iterations = 1000;
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        igniteClient.compute().execute(name, params);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        long value = igniteClient.compute().execute(name, new TestTaskParams(CACHE_NAME, "42", 0));
        if (value == threads * iterations) {
            System.out.println("Implementation which you use is compatible with Bucket4j");
        } else {
            String msg = "Implementation which you use is not compatible with Bucket4j";
            msg += ", " + (threads * iterations - value) + " writes are missed";
            throw new IllegalStateException(msg);
        }
    }

}
