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

import io.github.bucket4j.AbstractDistributedBucketTest;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.RecoveryStrategy;
import org.infinispan.functional.FunctionalMap.ReadWriteMap;
import org.infinispan.functional.impl.FunctionalMapImpl;
import org.infinispan.functional.impl.ReadWriteMapImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;


public class InfinispanTest extends AbstractDistributedBucketTest<InfinispanBucketBuilder, Infinispan> {

    private static ReadWriteMap<String, GridBucketState> readWriteMap;
    private static Cache<String, GridBucketState> cache;
    private static CacheManager cacheManager1;
    private static CacheManager cacheManager2;

    @Test(expected = IllegalArgumentException.class)
    @Override
    public void testThatImpossibleToPassNullCacheToProxyManagerConstructor() {
        Bucket4j.extension(getExtensionClass()).proxyManagerForMap(null);
    }

    @BeforeClass
    public static void init() throws MalformedURLException, URISyntaxException {
        URI configurationUri = InfinispanTest.class.getResource("/infinispan-jcache-cluster.xml").toURI();
        ClassLoader tccl = InfinispanTest.class.getClassLoader();

        cacheManager1 = Caching.getCachingProvider().getCacheManager(configurationUri, new TestClassLoader(tccl));
        cache = cacheManager1.getCache("namedCache");
        readWriteMap = toMap(cache);

        cacheManager2 = Caching.getCachingProvider().getCacheManager(configurationUri, new TestClassLoader(tccl));
        cacheManager2.getCache("namedCache");
    }

    @AfterClass
    public static void destroy() {
        cacheManager1.close();
        cacheManager2.close();
    }

    @Override
    protected Class<Infinispan> getExtensionClass() {
        return Infinispan.class;
    }

    @Override
    protected Bucket build(InfinispanBucketBuilder builder, String key, RecoveryStrategy recoveryStrategy) {
        return builder.build(readWriteMap, key, recoveryStrategy);
    }

    @Override
    protected ProxyManager<String> newProxyManager() {
        return Bucket4j.extension(Infinispan.class).proxyManagerForMap(readWriteMap);
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

    private static ReadWriteMap<String, GridBucketState> toMap(Cache<String, GridBucketState> cache) {
        org.infinispan.Cache<String, GridBucketState> nativeCache = cache.unwrap(org.infinispan.Cache.class);
        FunctionalMapImpl<String, GridBucketState> functionalMap = FunctionalMapImpl.create(nativeCache.getAdvancedCache());
        return ReadWriteMapImpl.create(functionalMap);
    }

}
