
package io.github.bucket4j.grid.jcache.hazelcast;

import com.hazelcast.config.CacheSimpleConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICacheManager;

import io.github.bucket4j.grid.jcache.Bucket4jJCache;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

public class HazelcastJCacheTest extends AbstractDistributedBucketTest {

    private static Cache<String, byte[]> cache;
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
            CacheSimpleConfig cacheConfig = new CacheSimpleConfig();
            cacheConfig.setName("my_buckets");
            config.addCacheConfig(cacheConfig);

            HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            ICacheManager cacheManager = hazelcastInstance.getCacheManager();
            cacheManager.getCache("my_buckets");
        });

        // start hazelcast client which works inside current JVM and does not hold data
        Config config = new Config();
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true);
        joinConfig.getTcpIpConfig().addMember("127.0.0.1:5701");
        config.setLiteMember(true);
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        ICacheManager cacheManager = hazelcastInstance.getCacheManager();
        cache = cacheManager.getCache("my_buckets");

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "JCacheProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jJCache.entryProcessorBasedBuilder(getCache())
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

    private static Cache<String, byte[]> getCache() {
        return cache;
    }

}
