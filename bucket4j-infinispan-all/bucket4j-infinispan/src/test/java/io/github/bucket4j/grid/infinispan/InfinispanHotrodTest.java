package io.github.bucket4j.grid.infinispan;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfiguration;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.github.bucket4j.grid.infinispan.serialization.Bucket4jProtobufContextInitializer;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;

public class InfinispanHotrodTest extends AbstractDistributedBucketTest {

    private static Cache<String, byte[]> cache;
    private static DefaultCacheManager cacheManager;
    private static HotRodServer hotrodServer;
    private static RemoteCacheManager remoteCacheManager;

    @BeforeAll
    public static void init() throws MalformedURLException, URISyntaxException {
        cacheManager = new DefaultCacheManager(getGlobalConfiguration());
        cacheManager.defineConfiguration("my-cache",
            new org.infinispan.configuration.cache.ConfigurationBuilder()
                .clustering()
                .cacheMode(CacheMode.DIST_SYNC)
                .hash().numOwners(1)
                .build()
        );
        cache = cacheManager.getCache("my-cache");

        // Create a Hot Rod server which exposes the cache manager
        HotRodServerConfiguration hotrodServerConfig = new HotRodServerConfigurationBuilder().build();
        hotrodServer = new HotRodServer();
        hotrodServer.start(hotrodServerConfig, cacheManager);

        // Create a Hot Rod client
        org.infinispan.client.hotrod.configuration.ConfigurationBuilder hotrodClientConfigBuilder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
        hotrodClientConfigBuilder.addServers("localhost");
        Configuration hotrodClientConfig = hotrodClientConfigBuilder.build();
        remoteCacheManager = new RemoteCacheManager(hotrodClientConfig);
        RemoteCache<String, byte[]> remoteCache = remoteCacheManager.getCache("my-cache");

        specs = Arrays.asList(
//            new ProxyManagerSpec<>(
//                "InfinispanProxyManager",
//                () -> UUID.randomUUID().toString(),
//                () -> Bucket4jInfinispan.entryProcessorBasedBuilder(readWriteMap)
//            ).checkExpiration()
        );


    }

    private static GlobalConfiguration getGlobalConfiguration() {
        GlobalConfigurationBuilder globalConfigurationBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
        globalConfigurationBuilder.serialization().addContextInitializer(new Bucket4jProtobufContextInitializer());
        return globalConfigurationBuilder.build();
    }

    @AfterAll
    public static void destroy() throws IOException {
        try {
            remoteCacheManager.stop();
        } finally {
            try {
                hotrodServer.stop();
            } finally {
                cacheManager.close();
            }
        }
    }

}
