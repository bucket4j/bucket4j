package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.infinispan.commons.api.functional.FunctionalMap.ReadWriteMap;
import org.infinispan.functional.impl.FunctionalMapImpl;
import org.infinispan.functional.impl.ReadWriteMapImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;


public class InfinispanTest extends AbstractDistributedBucketTest {

    private static ReadWriteMap<String, byte[]> readWriteMap;
    private static Cache<String, byte[]> cache;
    private static CacheManager cacheManager1;
    private static CacheManager cacheManager2;

    @BeforeClass
    public static void init() throws MalformedURLException, URISyntaxException {
        URI configurationUri = InfinispanTest.class.getResource("/infinispan-jcache-cluster.xml").toURI();
        ClassLoader tccl = InfinispanTest.class.getClassLoader();

        CachingProvider cachingProvider = Caching.getCachingProvider("org.infinispan.jcache.embedded.JCachingProvider");
        cacheManager1 = cachingProvider.getCacheManager(configurationUri, new TestClassLoader(tccl));
        cache = cacheManager1.getCache("namedCache");
        readWriteMap = toMap(cache);

        cacheManager2 = cachingProvider.getCacheManager(configurationUri, new TestClassLoader(tccl));
        cacheManager2.getCache("namedCache");
    }

    @AfterClass
    public static void destroy() {
        cacheManager1.close();
        cacheManager2.close();
    }

    @Override
    protected Backend<String> getBackend() {
        return new InfinispanBackend<>(readWriteMap);
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        cache.remove(key);
    }

    public static class TestClassLoader extends ClassLoader {
        public TestClassLoader(ClassLoader parent) {
            super(parent);
        }
    }

    private static ReadWriteMap<String, byte[]> toMap(Cache<String, byte[]> cache) {
        org.infinispan.Cache<String, byte[]> nativeCache = cache.unwrap(org.infinispan.Cache.class);
        FunctionalMapImpl<String, byte[]> functionalMap = FunctionalMapImpl.create(nativeCache.getAdvancedCache());
        return ReadWriteMapImpl.create(functionalMap);
    }

}
