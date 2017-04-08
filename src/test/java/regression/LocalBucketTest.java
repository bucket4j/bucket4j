package regression;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
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
        Bucket bucket = Bucket4j.builder()
                .withNanosecondPrecision()
                .addLimit(0, Bandwidth.simple(100, Duration.ofMillis(1)))
                .build();
        bucket.consume(1);
    }

}
