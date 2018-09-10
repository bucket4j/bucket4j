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

package io.github.bucket4j.grid.jcache.hazelcast;

import com.hazelcast.config.CacheSimpleConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICacheManager;
import io.github.bucket4j.grid.jcache.AbstractJCacheTest;
import io.github.bucket4j.remote.RemoteBucketState;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.cache.Cache;
import java.io.Serializable;

public class HazelcastJCacheTest extends AbstractJCacheTest {

    private static Cache<String, RemoteBucketState> cache;
    private static Cloud cloud;
    private static ViNode server;

    private static HazelcastInstance hazelcastInstance;

    @BeforeClass
    public static void setup() {
        // start separated JVM on current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        server = cloud.node("stateful-hazelcast-server");

        server.exec((Runnable & Serializable) () -> {
            Config config = new Config();
            config.setLiteMember(false);
            CacheSimpleConfig cacheConfig = new CacheSimpleConfig();
            cacheConfig.setName("my_buckets");
            config.addCacheConfig(cacheConfig);

            HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            ICacheManager cacheManager = hazelcastInstance.getCacheManager();
            cacheManager.getCache("my_buckets");
        });

        // start hazelcast client which works inside current JVM and does not hold data
        Config config = new Config();
        config.setLiteMember(true);
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        ICacheManager cacheManager = hazelcastInstance.getCacheManager();
        cache = cacheManager.getCache("my_buckets");
    }

    @AfterClass
    public static void shutdown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
        if (cloud != null) {
            cloud.shutdown();
        }
    }

    @Override
    protected Cache<String, RemoteBucketState> getCache() {
        return cache;
    }

}
