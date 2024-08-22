

package io.github.bucket4j.grid.infinispan;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.functional.FunctionalMap.ReadWriteMap;
import org.infinispan.functional.impl.FunctionalMapImpl;
import org.infinispan.functional.impl.ReadWriteMapImpl;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.github.bucket4j.grid.infinispan.serialization.Bucket4jProtobufContextInitializer;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.AsyncProxyManagerSpec;
import io.github.bucket4j.tck.ProxyManagerSpec;


public class InfinispanTest extends AbstractDistributedBucketTest {

    private static ReadWriteMap<String, byte[]> readWriteMap;
    private static Cache<String, byte[]> cache;
    private static DefaultCacheManager cacheManager1;
    private static DefaultCacheManager cacheManager2;

    @BeforeAll
    public static void init() throws MalformedURLException, URISyntaxException {
        cacheManager1 = new DefaultCacheManager(getGlobalConfiguration());
        cacheManager1.defineConfiguration("my-cache",
                new ConfigurationBuilder()
                        .clustering()
                        .cacheMode(CacheMode.DIST_SYNC)
                        .hash().numOwners(2)
                        .build()
        );

        cache = cacheManager1.getCache("my-cache");
        readWriteMap = toMap(cache);

        cacheManager2 = new DefaultCacheManager(getGlobalConfiguration());
        cacheManager2.defineConfiguration("my-cache",
                new ConfigurationBuilder()
                        .clustering()
                        .cacheMode(CacheMode.DIST_SYNC)
                        .hash().numOwners(2)
                        .build()
        );
        cacheManager2.getCache("my-cache");

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "InfinispanProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jInfinispan.entryProcessorBasedBuilder(readWriteMap)
            ).checkExpiration()
        );

        asyncSpecs = Arrays.asList(
            new AsyncProxyManagerSpec<>(
                "InfinispanAsyncProxyManager",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jInfinispan.asyncEntryProcessorBasedBuilder(readWriteMap)
            ).checkExpiration()
        );
    }

    private static GlobalConfiguration getGlobalConfiguration() {
        GlobalConfigurationBuilder globalConfigurationBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
        globalConfigurationBuilder.serialization().addContextInitializer(new Bucket4jProtobufContextInitializer());
        return globalConfigurationBuilder.build();
    }

    @AfterAll
    public static void destroy() throws IOException {
        cacheManager1.close();
        cacheManager2.close();
    }

    private static ReadWriteMap<String, byte[]> toMap(Cache<String, byte[]> cache) {
        FunctionalMapImpl<String, byte[]> functionalMap = FunctionalMapImpl.create(cache.getAdvancedCache());
        return ReadWriteMapImpl.create(functionalMap);
    }

}
