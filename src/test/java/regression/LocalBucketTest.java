package regression;

import com.github.bucket4j.Bucket;
import com.github.bucket4j.BucketBuilder;
import org.junit.Test;

import java.time.Duration;

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
        Bucket bucket = BucketBuilder.forNanosecondPrecision().withLimitedBandwidth(100, Duration.ofMillis(1)).build();
        bucket.consumeSingleToken();
    }

}
