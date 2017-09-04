/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.grid.jcache.infinispan;

import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.jcache.AbstractJCacheTest;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.net.URISyntaxException;

public class InfinispanJCacheTest extends AbstractJCacheTest {

    static URI configurationUri = null;

    static {
        try {
            configurationUri = InfinispanJCacheTest.class.getResource("/infinispan-jcache-cluster.xml").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Cache<String, GridBucketState> cache1;
    private static CacheManager cacheManager1;
    private static CacheManager cacheManager2;

    @BeforeClass
    public static void setup() {
        ClassLoader tccl = InfinispanJCacheTest.class.getClassLoader();
        CachingProvider cachingProvider1 = Caching.getCachingProvider("org.infinispan.jcache.embedded.JCachingProvider");
        cacheManager1 = cachingProvider1.getCacheManager(configurationUri, new TestClassLoader(tccl));
        cache1 = cacheManager1.getCache("my_buckets");

//        CachingProvider cachingProvider2 = Caching.getCachingProvider("org.infinispan.jcache.embedded.JCachingProvider");
//        cacheManager2 = cachingProvider2.getCacheManager(configurationUri, new TestClassLoader(tccl));
//        cacheManager2.getCache("my_buckets");
    }

    @AfterClass
    public static void shutdown() {
        if (cacheManager1 != null) {
            cacheManager1.close();
        }
        if (cacheManager2 != null) {
            cacheManager2.close();
        }
    }

    @Override
    protected Cache<String, GridBucketState> getCache() {
        return cache1;
    }

    public static class TestClassLoader extends ClassLoader {
        public TestClassLoader(ClassLoader parent) {
            super(parent);
        }
    }

}
