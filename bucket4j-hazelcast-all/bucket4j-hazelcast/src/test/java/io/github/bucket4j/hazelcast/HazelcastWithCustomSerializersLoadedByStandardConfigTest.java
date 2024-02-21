package io.github.bucket4j.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import groovy.util.logging.Slf4j;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.serialization.JsonOutputSerializationTest;
import io.github.bucket4j.grid.hazelcast.HazelcastEntryProcessor;
import io.github.bucket4j.grid.hazelcast.HazelcastOffloadableEntryProcessor;
import io.github.bucket4j.grid.hazelcast.HazelcastProxyManager;
import io.github.bucket4j.grid.hazelcast.SimpleBackupProcessor;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastEntryProcessorSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastOffloadableEntryProcessorSerializer;
import io.github.bucket4j.grid.hazelcast.serialization.SerializationUtilities;
import io.github.bucket4j.grid.hazelcast.serialization.SimpleBackupProcessorSerializer;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

public class HazelcastWithCustomSerializersLoadedByStandardConfigTest extends AbstractDistributedBucketTest {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastWithCustomSerializersLoadedByStandardConfigTest.class);

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

            // Added here the System Property (or the Env Var) as it could happen with the standalone cluster
            System.setProperty(SerializationUtilities.TYPE_ID_BASE_PROP_NAME, "10000");
            String testSP = System.getProperty(SerializationUtilities.TYPE_ID_BASE_PROP_NAME);
            logger.info("The System Property [{}] has the following value [{}]", SerializationUtilities.TYPE_ID_BASE_PROP_NAME, testSP);

            // *******************************************************************************************************
            // with this block we are simulating the load of the hazelcast config by the config_file since we are using only the
            // serializer parameterless constructor as it required by the config file
            config.getSerializationConfig().addSerializerConfig(
                    new SerializerConfig()
                            .setClass(HazelcastEntryProcessorSerializer.class)
                            .setTypeClass(HazelcastEntryProcessor.class)
            );
            config.getSerializationConfig().addSerializerConfig(
                    new SerializerConfig()
                            .setClass(SimpleBackupProcessorSerializer.class)
                            .setTypeClass(SimpleBackupProcessor.class)
            );
            config.getSerializationConfig().addSerializerConfig(
                    new SerializerConfig()
                            .setClass(HazelcastOffloadableEntryProcessorSerializer.class)
                            .setTypeClass(HazelcastOffloadableEntryProcessor.class)
            );
            // *******************************************************************************************************

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
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true);
        joinConfig.getTcpIpConfig().addMember("127.0.0.1:5701");
        HazelcastProxyManager.addCustomSerializers(config.getSerializationConfig(), 10_000);
        config.setLiteMember(true);
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        map = hazelcastInstance.getMap("my_buckets");

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "HazelcastProxyManager_CustomSerialization",
                () -> UUID.randomUUID().toString(),
                new HazelcastProxyManager<>(map, ClientSideConfig.getDefault())
            ),
            new ProxyManagerSpec<>(
                "HazelcastLockBasedProxyManager_JdkSerialization_offloadableExecutor",
                () -> UUID.randomUUID().toString(),
                new HazelcastProxyManager<>(map, ClientSideConfig.getDefault(), "my-executor")
            )
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
