package realworld.grid;

import com.github.bandwidthlimiter.Buckets;
import com.github.bandwidthlimiter.bucket.Bucket;
import com.github.bandwidthlimiter.bucket.BucketState;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.littlegrid.ClusterMemberGroup;
import org.littlegrid.ClusterMemberGroupUtils;
import realworld.ConsumptionScenario;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class CoherenceTest {

    private static final String KEY = "42";

    private static ClusterMemberGroup memberGroup;
    private static NamedCache cache;

    @BeforeClass
    public static void prepareCache() throws InterruptedException {
        memberGroup = ClusterMemberGroupUtils.newBuilder().setStorageEnabledCount(2)
                .setCacheConfiguration("test-coherence-config.xml")
                .buildAndConfigureForStorageDisabledClient();
        cache = CacheFactory.getCache("my_buckets");
    }

    @AfterClass
    public static void shutdownCache() {
        ClusterMemberGroupUtils.shutdownCacheFactoryThenClusterMemberGroups(memberGroup);
    }

    @Test
    public void test15Seconds() throws Exception {
        Bucket bucket = Buckets.withNanoTimePrecision()
                .withLimitedBandwidth(1_000l, TimeUnit.MINUTES.toNanos(1), 0)
                .withLimitedBandwidth(200l, TimeUnit.SECONDS.toNanos(10), 0)
                .buildCoherence(cache, KEY);

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
