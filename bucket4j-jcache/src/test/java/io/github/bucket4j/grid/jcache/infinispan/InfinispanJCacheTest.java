
package io.github.bucket4j.grid.jcache.infinispan;

import io.github.bucket4j.grid.jcache.AbstractJCacheTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadLocalRandom;

public class InfinispanJCacheTest extends AbstractJCacheTest {

    static URI configurationUri = null;

    static {
        try {
            configurationUri = InfinispanJCacheTest.class.getResource("/infinispan-jcache-cluster.xml").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Cache<String, byte[]> cache1;
    private static CacheManager cacheManager1;

    private static CacheManager cacheManager2;
    private static Cache<String, byte[]> cache2;
    private static TestClassLoader1 classLoader1 = new TestClassLoader1(InfinispanJCacheTest.class.getClassLoader());
    private static TestClassLoader2 classLoader2 = new TestClassLoader2(InfinispanJCacheTest.class.getClassLoader());

    @BeforeAll
    public static void setup() {
        CachingProvider cachingProvider = Caching.getCachingProvider("org.infinispan.jcache.embedded.JCachingProvider");

        cacheManager1 = cachingProvider.getCacheManager(configurationUri, classLoader1);
        cache1 = cacheManager1.getCache("my_buckets");

        cacheManager2 = cachingProvider.getCacheManager(configurationUri, classLoader2);
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

    @Override
    protected Cache<String, byte[]> getCache() {
        return ThreadLocalRandom.current().nextBoolean()? cache1 : cache2;
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
