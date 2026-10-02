package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.grid.ignite.thin.ThinClientUtils;
import io.github.bucket4j.grid.ignite.thin.compute.Bucket4jComputeTask;
import io.github.bucket4j.grid.ignite.thin.compute.Bucket4jComputeTaskParams;
import io.github.bucket4j.grid.ignite.thin.compute.IgniteEntryProcessor;
import io.github.bucket4j.tck.BackwardCompatibilityRequestCheckHelper;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


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

        BackwardCompatibilityStateCheckHelper<String> backwardCompatibilityHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                return cache.get(key);
            }

            @Override
            public void setRawState(String key, byte[] state) {
                cache.put(key, state);
            }
        };

        BackwardCompatibilityStateCheckHelper<String> backwardCompatibilityHelper2 = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                ByteBuffer persistedState = cache2.get(key);
                return persistedState == null ? null : persistedState.array();
            }

            @Override
            public void setRawState(String key, byte[] state) {
                cache2.put(key, ByteBuffer.wrap(state));
            }
        };

        BackwardCompatibilityRequestCheckHelper<String> requestCompatibilityHelper = new BackwardCompatibilityRequestCheckHelper<>() {
            @Override
            public byte[] execute(String key, byte[] requestBytes) throws ExecutionException, InterruptedException {
                return igniteClient.compute().execute(Bucket4jComputeTask.JOB_NAME, newTaskParams(key, requestBytes));
            }

            @Override
            public CompletableFuture<byte[]> executeAsync(String key, byte[] requestBytes) {
                return ThinClientUtils.convertFuture(igniteClient.compute().<Bucket4jComputeTaskParams<String>, byte[]>executeAsync2(Bucket4jComputeTask.JOB_NAME, newTaskParams(key, requestBytes)));
            }

            private Bucket4jComputeTaskParams<String> newTaskParams(String key, byte[] requestBytes) {
                return new Bucket4jComputeTaskParams<>(CACHE_NAME, key, new IgniteEntryProcessor<>(requestBytes));
            }
        };

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "IgniteThinClientCompute",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite.thinClient().clientComputeBasedBuilder(cache, igniteClient.compute())
            ).checkStateBackwardCompatibility(backwardCompatibilityHelper).checkRequestBackwardCompatibility(requestCompatibilityHelper),
            new ProxyManagerSpec<>(
                "IgniteThinClientCas",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jIgnite.thinClient().casBasedBuilder(cache2)
            ).checkStateBackwardCompatibility(backwardCompatibilityHelper2).withoutBackwardCompatibilityRequestChecker()
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
