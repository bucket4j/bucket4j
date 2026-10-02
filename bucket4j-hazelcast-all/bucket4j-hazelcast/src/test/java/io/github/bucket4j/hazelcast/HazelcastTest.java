package io.github.bucket4j.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import io.github.bucket4j.grid.hazelcast.Bucket4jHazelcast;
import io.github.bucket4j.grid.hazelcast.HazelcastEntryProcessor;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityRequestCheckHelper;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

        BackwardCompatibilityStateCheckHelper<String> backwardCompatibilityHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                return map.get(key);
            }

            @Override
            public void setRawState(String key, byte[] state) {
                map.put(key, state);
            }
        };

        BackwardCompatibilityRequestCheckHelper<String> requestCompatibilityHelper = new BackwardCompatibilityRequestCheckHelper<>() {
            @Override
            public byte[] execute(String key, byte[] requestBytes) {
                return map.executeOnKey(key, new HazelcastEntryProcessor<String, Object>(requestBytes));
            }

            @Override
            public CompletableFuture<byte[]> executeAsync(String key, byte[] requestBytes) {
                return map.submitToKey(key, new HazelcastEntryProcessor<String, Object>(requestBytes)).toCompletableFuture();
            }
        };

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "HazelcastProxyManager_JdkSerialization",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jHazelcast.entryProcessorBasedBuilder(map)
            ).checkExpiration().checkStateBackwardCompatibility(backwardCompatibilityHelper).checkRequestBackwardCompatibility(requestCompatibilityHelper),
            new ProxyManagerSpec<>(
                "HazelcastLockBasedProxyManager_JdkSerialization",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jHazelcast.lockBasedBuilder(map)
            ).checkExpiration().checkStateBackwardCompatibility(backwardCompatibilityHelper).withoutBackwardCompatibilityRequestChecker(),
            new ProxyManagerSpec<>(
                "HazelcastCompareAndSwapBasedProxyManager_JdkSerialization",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jHazelcast.casBasedBuilder(map)
            ).checkStateBackwardCompatibility(backwardCompatibilityHelper).withoutBackwardCompatibilityRequestChecker()
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
