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

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.AbstractDistributedBucketTest;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.grid.infinispan.serialization.Bucket4jProtobufContextInitializer;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.functional.FunctionalMap.ReadWriteMap;
import org.infinispan.functional.impl.FunctionalMapImpl;
import org.infinispan.functional.impl.ReadWriteMapImpl;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;


public class InfinispanTest extends AbstractDistributedBucketTest {

    private static ReadWriteMap<String, byte[]> readWriteMap;
    private static Cache<String, byte[]> cache;
    private static DefaultCacheManager cacheManager1;
    private static DefaultCacheManager cacheManager2;

    @BeforeClass
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
    }

    private static GlobalConfiguration getGlobalConfiguration() {
        GlobalConfigurationBuilder globalConfigurationBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
        globalConfigurationBuilder.serialization().addContextInitializer(new Bucket4jProtobufContextInitializer());
        return globalConfigurationBuilder.build();
    }

    @AfterClass
    public static void destroy() throws IOException {
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

    private static ReadWriteMap<String, byte[]> toMap(Cache<String, byte[]> cache) {
        FunctionalMapImpl<String, byte[]> functionalMap = FunctionalMapImpl.create(cache.getAdvancedCache());
        return ReadWriteMapImpl.create(functionalMap);
    }

}
