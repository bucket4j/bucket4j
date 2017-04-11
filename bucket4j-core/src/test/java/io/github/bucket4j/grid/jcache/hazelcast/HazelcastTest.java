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
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.RecoveryStrategy;
import com.hazelcast.config.CacheSimpleConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import realworld.ConsumptionScenario;

import javax.cache.Cache;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HazelcastTest {

    private static final String KEY = "42";
    private Cache<String, GridBucketState> cache;
    private HazelcastInstance hazelcastInstance;

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
    public void test15Seconds() throws Exception {
        Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .addLimit(0, Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                .addLimit(0, Bandwidth.simple(200, Duration.ofSeconds(10)))
                .build(cache, KEY);

        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucket);
        long consumed = scenario.execute();
        long duration = scenario.getDurationNanos();
        System.out.println("Consumed " + consumed + " tokens in the " + duration + " nanos");

        float actualRate = (float) consumed / (float) duration;
        float permittedRate = 200.0f / (float) TimeUnit.SECONDS.toNanos(10);

        String msg = "Actual rate " + actualRate + " is greater then permitted rate " + permittedRate;
        assertTrue(msg, actualRate <= permittedRate);

        BucketState snapshot = bucket.createSnapshot();
        BucketConfiguration configuration = bucket.getConfiguration();
        long available = snapshot.getAvailableTokens(configuration.getBandwidths());
        long rest = bucket.tryConsumeAsMuchAsPossible();
        assertTrue(rest >= available);
    }

}
