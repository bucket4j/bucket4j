package io.github.bucket4j.grid.jcache.compatibility_investigation.infinispan;


import io.github.bucket4j.grid.jcache.compatibility_investigation.CompatibilityTest;
import io.github.bucket4j.grid.jcache.infinispan.InfinispanJCacheTest;
import org.ehcache.jsr107.EhcacheCachingProvider;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.net.URISyntaxException;

public class InfinispanCompatibilityInvestigation {

    public static void main(String[] args) throws InterruptedException, URISyntaxException {
        URI configurationUri = InfinispanJCacheTest.class.getResource("/infinispan-jcache-cluster.xml").toURI();
        ClassLoader tccl = InfinispanJCacheTest.class.getClassLoader();
        CachingProvider cachingProvider = Caching.getCachingProvider("org.infinispan.jcache.embedded.JCachingProvider");
        CacheManager cacheManager = cachingProvider.getCacheManager(configurationUri, new InfinispanJCacheTest.TestClassLoader(tccl));
        Cache<String, Integer> cache = cacheManager.getCache("my_buckets");

        CompatibilityTest test = new CompatibilityTest(cache);
        test.test();
    }

}
