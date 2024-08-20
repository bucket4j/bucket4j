package io.github.bucket4j.hazelcast;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import io.github.bucket4j.grid.hazelcast.Bucket4jHazelcast;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.AsyncProxyManagerSpec;
import io.github.bucket4j.tck.ProxyManagerSpec;

public class HazelcastTest extends AbstractDistributedBucketTest {

    private static IMap<String, byte[]> map;
    private static Cloud cloud;
    private static ViNode server;

    private static HazelcastInstance hazelcastInstance;

    @BeforeAll
    public static void setup() {
        // start separated JVM on current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        server = cloud.node("stateful-hazelcast-server");

        server.exec((Runnable & Serializable) () -> {
            Config config = new Config();
            JoinConfig joinConfig = config.getNetworkConfig().getJoin();
            joinConfig.getMulticastConfig().setEnabled(false);
            joinConfig.getTcpIpConfig().setEnabled(true);
            joinConfig.getTcpIpConfig().addMember("127.0.0.1:5702");
            config.setLiteMember(false);
            HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            hazelcastInstance.getMap("my_buckets");
        });

        // start hazelcast client which works inside current JVM and does not hold data
        Config config = new Config();
        config.setLiteMember(true);
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true);
        joinConfig.getTcpIpConfig().addMember("127.0.0.1:5701");
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        map = hazelcastInstance.getMap("my_buckets");

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "HazelcastProxyManager_JdkSerialization",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jHazelcast.entryProcessorBasedBuilder(map)
            ).checkExpiration(),
            new ProxyManagerSpec<>(
                "HazelcastLockBasedProxyManager_JdkSerialization",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jHazelcast.lockBasedBuilder(map)
            ).checkExpiration(),
            new ProxyManagerSpec<>(
                "HazelcastCompareAndSwapBasedProxyManager_JdkSerialization",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jHazelcast.casBasedBuilder(map)
            )
        );

        asyncSpecs = Arrays.asList(
            new AsyncProxyManagerSpec<>(
                "AsyncHazelcastProxyManager_JdkSerialization",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jHazelcast.asyncEntryProcessorBasedBuilder(map)
            ).checkExpiration()
        );
    }

    @AfterAll
    public static void shutdown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
        if (cloud != null) {
            cloud.shutdown();
        }
    }
}
