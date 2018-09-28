/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.grid.jcache.infinispan;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.grid.jcache.JCacheBackend;
import io.github.bucket4j.remote.RecoveryStrategy;
import io.github.bucket4j.remote.RemoteBucketState;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

public class InfinispanJCacheTest {

    static URI configurationUri = null;

    static {
        try {
            configurationUri = InfinispanJCacheTest.class.getResource("/infinispan-jcache-cluster.xml").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Cache<String, RemoteBucketState> cache;
    private static CacheManager cacheManager;

    @BeforeClass
    public static void setup() {
        ClassLoader tccl = InfinispanJCacheTest.class.getClassLoader();
        CachingProvider cachingProvider = Caching.getCachingProvider("org.infinispan.jcache.embedded.JCachingProvider");
        cacheManager = cachingProvider.getCacheManager(configurationUri, new TestClassLoader(tccl));
        cache = cacheManager.getCache("my_buckets");

    }

    @AfterClass
    public static void shutdown() {
        if (cacheManager != null) {
            cacheManager.close();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void checkThatInfinispanProviderUnsupported() {
        Bucket4j.builder(new JCacheBackend<>(cache))
                .addLimit(Bandwidth.simple(10, Duration.ofSeconds(60)))
                .build("42", RecoveryStrategy.RECONSTRUCT);
    }

    public static class TestClassLoader extends ClassLoader {
        public TestClassLoader(ClassLoader parent) {
            super(parent);
        }
    }

}
