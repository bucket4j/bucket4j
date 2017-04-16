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

package io.github.bucket4j.grid.jcache.hazelcast;

import io.github.bucket4j.*;
import io.github.bucket4j.grid.BucketNotFoundException;
import io.github.bucket4j.grid.BucketRegistry;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.RecoveryStrategy;
import com.hazelcast.config.CacheSimpleConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICacheManager;
import io.github.bucket4j.grid.jcache.JCacheBucketRegistry;
import io.github.bucket4j.grid.jcache.JCacheConfigurationBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import io.github.bucket4j.util.ConsumptionScenario;

import javax.cache.Cache;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HazelcastTest {

    private static final String KEY = "42";
    private static final String ANOTHER_KEY = "666";
    private Cache<String, GridBucketState> cache;
    private HazelcastInstance hazelcastInstance;
    private JCacheConfigurationBuilder builder = Bucket4j.jCacheBuilder(RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION)
            .addLimit(0, Bandwidth.simple(1_000, Duration.ofMinutes(1)))
            .addLimit(0, Bandwidth.simple(200, Duration.ofSeconds(10)));
    private double permittedRatePerSecond = Math.min(1_000d / 60, 200.0 / 10);

    @Before
    public void setup() {
        Config config = new Config();
        CacheSimpleConfig cacheConfig = new CacheSimpleConfig();
        cacheConfig.setName("my_buckets");
        config.addCacheConfig(cacheConfig);

        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        ICacheManager cacheManager = hazelcastInstance.getCacheManager();
        cache = cacheManager.getCache("my_buckets");
    }

    @After
    public void shutdown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }

    @Test
    public void testJCacheBucketRegistryWithKeyIndependentConfiguration() {
        BucketConfiguration configuration = Bucket4j.configurationBuilder()
                .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                .createConfiguration();

        BucketRegistry<String> registry = JCacheBucketRegistry.withKeyIndependentConfiguration(cache, configuration);
        Bucket bucket1 = registry.getProxy(KEY);
        assertTrue(bucket1.tryConsume(10));
        assertFalse(bucket1.tryConsume(1));

        Bucket bucket2 = registry.getProxy(ANOTHER_KEY);
        assertTrue(bucket2.tryConsume(10));
        assertFalse(bucket2.tryConsume(1));
    }

    @Test
    public void testJCacheBucketRegistryWithKeyDependentConfiguration() {
        Function<String, BucketConfiguration> supplier = (key) -> {
            if (key.equals(KEY)) {
                return Bucket4j.configurationBuilder()
                        .addLimit(Bandwidth.simple(10, Duration.ofDays(1)))
                        .createConfiguration();
            }
            if (key.equals(ANOTHER_KEY)) {
                return Bucket4j.configurationBuilder()
                        .addLimit(Bandwidth.simple(100, Duration.ofDays(1)))
                        .createConfiguration();
            }
            throw new IllegalStateException();
        };

        BucketRegistry<String> registry = JCacheBucketRegistry.withKeyDependentConfiguration(cache, supplier);
        Bucket bucket1 = registry.getProxy(KEY);
        assertFalse(bucket1.tryConsume(11));

        Bucket bucket2 = registry.getProxy(ANOTHER_KEY);
        assertTrue(bucket2.tryConsume(11));
    }

    @Test
    public void testReconstructRecoveryStrategy() {
        Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.RECONSTRUCT)
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(cache, KEY);

        assertTrue(bucket.tryConsume(1));

        // simulate crash
        cache.remove(KEY);

        assertTrue(bucket.tryConsume(1));
    }

    @Test
    public void testThrowExceptionRecoveryStrategy() {
        Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(cache, KEY);

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
        Supplier<Bucket> bucketSupplier = () -> builder.build(cache, KEY);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

    @Test
    public void testConsume() throws Exception {
        Function<Bucket, Long> action = bucket -> {
            bucket.consumeUninterruptibly(1);
            return 1L;
        };
        Supplier<Bucket> bucketSupplier = () -> builder.build(cache, KEY);
        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucketSupplier, action, permittedRatePerSecond);
        scenario.executeAndValidateRate();
    }

}
