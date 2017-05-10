/*
 *  Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.github.bucket4j.grid.jcache.ignite;

import io.github.bucket4j.*;
import io.github.bucket4j.grid.BucketNotFoundException;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.jcache.JCache;
import io.github.bucket4j.grid.jcache.JCacheBucketBuilder;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.github.bucket4j.util.ConsumptionScenario;

import javax.cache.Cache;
import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.bucket4j.grid.RecoveryStrategy.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IgniteTest {

    private static final String KEY = "42";
    private Ignite ignite;
    private Cache<String, GridBucketState> cache;
    private Cloud cloud;


    private JCacheBucketBuilder builder = Bucket4j.extension(JCache.class).builder()
            .addLimit(0, Bandwidth.simple(1_000, Duration.ofMinutes(1)))
            .addLimit(0, Bandwidth.simple(200, Duration.ofSeconds(10)));
    private double permittedRatePerSecond = Math.min(1_000d / 60, 200.0 / 10);

    @Before
    public void setup() {
        // start ignite server on current host
        cloud = CloudFactory.createCloud();
        cloud.node("**").x(VX.TYPE).setLocal();
        ViNode igniteServer = cloud.node("ignite-server");
        igniteServer.x(VX.JVM).addJvmArg("-DIGNITE_QUIET=false"); // show verbose output
//        igniteServer.x(VX.CONSOLE).bindErr(System.err);
//        igniteServer.x(VX.CONSOLE).bindOut(System.out);

        Runnable startIgniteServer = getStartIgniteServerCommand();
        igniteServer.exec(startIgniteServer);

        // start ignite client which works inside current JVM
        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
        igniteConfiguration.setClientMode(true);
        ignite = Ignition.start(igniteConfiguration);

        CacheConfiguration cacheConfiguration = new CacheConfiguration("my_buckets");
        cache = ignite.getOrCreateCache(cacheConfiguration);
    }

    private static Runnable getStartIgniteServerCommand() {
        return (Runnable & Serializable) () -> {
            IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
            igniteConfiguration.setClientMode(false);
            CacheConfiguration cacheConfiguration = new CacheConfiguration("my_buckets");
            Ignite ignite = Ignition.start(igniteConfiguration);
            ignite.getOrCreateCache(cacheConfiguration);
        };
    }

    @After
    public void shutdown() {
        try {
            if (cloud != null) {
                cloud.shutdown();
            }
        } finally {
            if (ignite != null) {
                ignite.close();
            }
        }
    }

    @Test
    public void testReconstructRecoveryStrategy() {
        Bucket bucket = Bucket4j.extension(JCache.class).builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(cache, KEY, RECONSTRUCT);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        cache.remove(KEY);

        assertTrue(bucket.tryConsume(1));
    }

    @Test
    public void testThrowExceptionRecoveryStrategy() {
        Bucket bucket = Bucket4j.extension(JCache.class).builder()
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(cache, KEY, THROW_BUCKET_NOT_FOUND_EXCEPTION);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        cache.remove(KEY);

        try {
            bucket.tryConsume(1);
            fail();
        } catch (BucketNotFoundException e) {
            // ok
        }
    }

    @Test
    public void testTryConsume() throws Exception {
        Function<Bucket, Long> action = bucket -> bucket.tryConsume(1)? 1L : 0L;
        Supplier<Bucket> bucketSupplier = () -> builder.build(cache, KEY, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testConsume() throws Exception {
        Function<Bucket, Long> action = bucket -> {
            bucket.consumeUninterruptibly(1, BlockingStrategy.PARKING);
            return 1L;
        };
        Supplier<Bucket> bucketSupplier = () -> builder.build(cache, KEY, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

}
