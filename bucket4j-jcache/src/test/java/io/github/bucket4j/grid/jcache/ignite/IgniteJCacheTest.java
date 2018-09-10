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

package io.github.bucket4j.grid.jcache.ignite;

import io.github.bucket4j.grid.jcache.AbstractJCacheTest;
import io.github.bucket4j.remote.RemoteBucketState;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.cache.Cache;
import java.io.Serializable;

public class IgniteJCacheTest extends AbstractJCacheTest {

    private static Cache<String, RemoteBucketState> cache;
    private static Cloud cloud;
    private static ViNode server;

    private static Ignite ignite;

    @BeforeClass
    public static void setup() {
        // start separated JVM on current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        server = cloud.node("stateful-ignite-server");

        server.exec((Runnable & Serializable) () -> {
            IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
            igniteConfiguration.setClientMode(false);
            CacheConfiguration cacheConfiguration = new CacheConfiguration("my_buckets");
            Ignite ignite = Ignition.start(igniteConfiguration);
            ignite.getOrCreateCache(cacheConfiguration);
        });

        // start ignite client which works inside current JVM and does not hold data
        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
        igniteConfiguration.setClientMode(true);
        ignite = Ignition.start(igniteConfiguration);
        CacheConfiguration cacheConfiguration = new CacheConfiguration("my_buckets");
        cache = ignite.getOrCreateCache(cacheConfiguration);
    }

    @AfterClass
    public static void shutdown() {
        if (ignite != null) {
            ignite.close();
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
