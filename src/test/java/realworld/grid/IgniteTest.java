package realworld.grid;

import com.github.bandwidthlimiter.Buckets;
import com.github.bandwidthlimiter.bucket.Bucket;
import com.github.bandwidthlimiter.bucket.BucketState;
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
