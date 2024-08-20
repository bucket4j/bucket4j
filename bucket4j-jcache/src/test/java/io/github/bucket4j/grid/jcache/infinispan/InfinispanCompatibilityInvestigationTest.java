package io.github.bucket4j.grid.jcache.infinispan;


import java.net.URI;
import java.net.URISyntaxException;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.bucket4j.grid.jcache.CompatibilityTest;

public class InfinispanCompatibilityInvestigationTest {

    public static final TestClassLoader1 CLASS_LOADER_1 = new TestClassLoader1(InfinispanJCacheTest.class.getClassLoader());
    public static final TestClassLoader2 CLASS_LOADER_2 = new TestClassLoader2(InfinispanJCacheTest.class.getClassLoader());
    static URI configurationUri = null;

    static {
        try {
            configurationUri = InfinispanJCacheTest.class.getResource("/infinispan-jcache-cluster.xml").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Cache<String, Integer> cache1;
    private static CacheManager cacheManager1;
    private static Cache<String, Integer> cache2;
    private static CacheManager cacheManager2;

    @BeforeAll
    public static void setup() {
        CachingProvider cachingProvider = Caching.getCachingProvider("org.infinispan.jcache.embedded.JCachingProvider");
        cacheManager1 = cachingProvider.getCacheManager(configurationUri, CLASS_LOADER_1);
        cache1 = cacheManager1.getCache("my_buckets");

        cacheManager2 = cachingProvider.getCacheManager(configurationUri, CLASS_LOADER_2);
        cache2 = cacheManager2.getCache("my_buckets");
    }

    @AfterAll
    public static void shutdown() {
        if (cacheManager1 != null) {
            cacheManager1.close();
        }
        if (cacheManager2 != null) {
            cacheManager2.close();
        }
    }

    @Test
    public void testCompatibility() throws InterruptedException, URISyntaxException {
        new CompatibilityTest(cache1).test();
        new CompatibilityTest(cache2).test();
    }

    public static class TestClassLoader1 extends ClassLoader {
        public TestClassLoader1(ClassLoader parent) {
            super(parent);
        }
    }

    public static class TestClassLoader2 extends ClassLoader {
        public TestClassLoader2(ClassLoader parent) {
            super(parent);
        }
    }

}
