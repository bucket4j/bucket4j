package regression;

import com.github.bucket4j.Bucket;
import com.github.bucket4j.Buckets;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class LocalBucketTest {

    /**
     * Found in the version 1.0.0
     *
     * Test that local bucket does not hang
     *
     * @throws InterruptedException
     */
    @Test(timeout = 3000)
    public void testConsumeOrAwait() throws InterruptedException {
        Bucket bucket = Buckets.withNanoTimePrecision().withLimitedBandwidth(100, TimeUnit.MILLISECONDS, 1, 0).build();
        bucket.consumeSingleToken();
    }

}
