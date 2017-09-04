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

package io.github.bucket4j.grid.infinispan;

import org.infinispan.functional.FunctionalMap.ReadWriteMap;
import org.infinispan.functional.impl.FunctionalMapImpl;
import org.infinispan.functional.impl.ReadWriteMapImpl;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import static io.github.bucket4j.grid.infinispan.InfinispanAsyncHelper.invokeAsync;
import static org.junit.Assert.assertEquals;

public class InfinispanAsyncHelperTest {

    private static Cache<String, Integer> localCache;
    private static ReadWriteMap<String, Integer> localReadWriteMap;
    private static Cache<String, Integer> distributedCache1;
    private static ReadWriteMap<String, Integer> readWriteMap1;
    private static Cache<String, Integer> distributedCache2;
    private static ReadWriteMap<String, Integer> readWriteMap2;


    private static CacheManager cacheManagerLocal;
    private static CacheManager cacheManager1;
    private static CacheManager cacheManager2;

    @BeforeClass
    public static void init() throws MalformedURLException, URISyntaxException {
        cacheManagerLocal = Caching.getCachingProvider(). getCacheManager();
        localCache = cacheManagerLocal.createCache("localCache", new MutableConfiguration<String, Integer>());
        localReadWriteMap = toMap(localCache);

        URI configurationUri = InfinispanAsyncHelperTest.class.getResource("/infinispan-jcache-cluster.xml").toURI();
        ClassLoader tccl = InfinispanAsyncHelperTest.class.getClassLoader();
        cacheManager1 = Caching.getCachingProvider().getCacheManager(configurationUri, new TestClassLoader(tccl));
        cacheManager2 = Caching.getCachingProvider().getCacheManager(configurationUri, new TestClassLoader(tccl));

        distributedCache1 = cacheManager1.getCache("namedCache");
        readWriteMap1 = toMap(distributedCache1);

        distributedCache2 = cacheManager2.getCache("namedCache");
        readWriteMap2 = toMap(distributedCache2);
    }

    private static ReadWriteMap<String, Integer> toMap(Cache<String, Integer> cache) {
        org.infinispan.Cache<String, Integer> nativeCache = cache.unwrap(org.infinispan.Cache.class);
        FunctionalMapImpl<String, Integer> functionalMap = FunctionalMapImpl.create(nativeCache.getAdvancedCache());
        return ReadWriteMapImpl.create(functionalMap);
    }


    public static class TestClassLoader extends ClassLoader {
        public TestClassLoader(ClassLoader parent) {
            super(parent);
        }
    }

    @Test
    public void testSuccessExecution() throws ExecutionException, InterruptedException {
        EntryProcessor<String, Integer, Long> entryProcessor = (EntryProcessor<String, Integer, Long> & Serializable) (MutableEntry<String, Integer> entry, Object... arguments) -> {
            if (!entry.exists()) {
                entry.setValue(1);
            } else {
                entry.setValue(entry.getValue() + 1);
            }
            return (long) entry.getValue() * 10;
        };
        assertEquals(10L, invokeAsync("42", localReadWriteMap, entryProcessor).get().longValue());
        assertEquals(20L, invokeAsync("42", localReadWriteMap, entryProcessor).get().longValue());

        assertEquals(10L, invokeAsync("42", readWriteMap1, entryProcessor).get().longValue());
        assertEquals(20L, invokeAsync("42", readWriteMap2, entryProcessor).get().longValue());
    }




}