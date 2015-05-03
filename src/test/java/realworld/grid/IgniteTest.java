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

package realworld.grid;

import com.github.bucket4j.Buckets;
import com.github.bucket4j.bucket.Bucket;
import com.github.bucket4j.bucket.BucketState;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import realworld.ConsumptionScenario;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class IgniteTest {

    private static final String KEY = "42";
    private Ignite ignite;
    private IgniteCache cache;

    @Before
    public void setup() {
        ignite = Ignition.start();

        CacheConfiguration cfg = new CacheConfiguration("my_buckets");
        cache = ignite.getOrCreateCache(cfg);
    }

    @After
    public void shutdown() {
        ignite.close();
    }

    @Test
    public void test15Seconds() throws Exception {
        Bucket bucket = Buckets.withNanoTimePrecision()
                .withLimitedBandwidth(1_000l, TimeUnit.MINUTES.toNanos(1), 0)
                .withLimitedBandwidth(200l, TimeUnit.SECONDS.toNanos(10), 0)
                .buildIgnite(cache, KEY);

        ConsumptionScenario scenario = new ConsumptionScenario(4, TimeUnit.SECONDS.toNanos(15), bucket);
        long consumed = scenario.execute();
        long duration = scenario.getDurationNanos();
        System.out.println("Consumed " + consumed + " tokens in the " + duration + " nanos");

        float actualRate = (float) consumed / (float) duration;
        float permittedRate = 200.0f / (float) TimeUnit.SECONDS.toNanos(10);

        String msg = "Actual rate " + actualRate + " is greater then permitted rate " + permittedRate;
        assertTrue(msg, actualRate <= permittedRate);

        BucketState snapshot = bucket.createSnapshot();
        long available = snapshot.getAvailableTokens(bucket.getConfiguration().getBandwidths());
        long rest = bucket.consumeAsMuchAsPossible();
        assertTrue(rest >= available);
    }

}
