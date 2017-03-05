/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package realworld.jcache;

import com.github.bucket4j.Bucket;
import com.github.bucket4j.Bucket4j;
import com.github.bucket4j.BucketState;
import com.github.bucket4j.grid.BucketNotFoundException;
import com.github.bucket4j.grid.GridBucketState;
import com.github.bucket4j.grid.RecoveryStrategy;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import realworld.ConsumptionScenario;

import javax.cache.Cache;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IgniteTest {

    private static final String KEY = "42";
    private Ignite ignite;
    private Cache<String, GridBucketState> cache;

    @Before
    public void setup() {
        ignite = Ignition.start();

        CacheConfiguration cfg = new CacheConfiguration("my_buckets");
        cache = ignite.getOrCreateCache(cfg);
    }

    @After
    public void shutdown() {
        if (ignite != null) {
            ignite.close();
        }
    }

    @Test
    public void testReconstructRecoveryStrategy() {
        Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.RECONSTRUCT)
                .withLimitedBandwidth(1_000, Duration.ofMinutes(1))
                .withLimitedBandwidth(200, Duration.ofSeconds(10))
                .build(cache, KEY);

        assertTrue(bucket.tryConsumeSingleToken());

        // simulate crash
        cache.remove(KEY);

        assertTrue(bucket.tryConsumeSingleToken());
    }

    @Test
    public void testThrowExceptionRecoveryStrategy() {
        Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .withLimitedBandwidth(1_000, Duration.ofMinutes(1))
                .withLimitedBandwidth(200, Duration.ofSeconds(10))
                .build(cache, KEY);

        assertTrue(bucket.tryConsumeSingleToken());

        // simulate crash
        cache.remove(KEY);

        try {
            bucket.tryConsumeSingleToken();
            fail();
        } catch (BucketNotFoundException e) {
            // ok
        }
    }

    @Test
    public void test15Seconds() throws Exception {
        Bucket bucket = Bucket4j.jCacheBuilder(RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .withLimitedBandwidth(1_000, 0, Duration.ofMinutes(1))
                .withLimitedBandwidth(200, 0, Duration.ofSeconds(10))
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
        long available = snapshot.getAvailableTokens(bucket.getConfiguration().getLimitedBandwidths());
        long rest = bucket.tryConsumeAsMuchAsPossible();
        assertTrue(rest >= available);
    }

}
