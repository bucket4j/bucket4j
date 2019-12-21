package io.github.bucket4j;

import com.hazelcast.nio.serialization.StreamSerializer;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

import static io.github.bucket4j.Bandwidth.classic;
import static io.github.bucket4j.Bandwidth.simple;
import static io.github.bucket4j.Refill.greedy;
import static io.github.bucket4j.Refill.intervallyAligned;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;

public class BucketStateSerializerTest extends SerializerTest<BucketState> {

    @Override
    protected StreamSerializer<BucketState> getSerializerUnderTest() {
        return this.bucketStateSerializer;
    }

    @Override
    protected void runAssertions(BucketState original, BucketState deserialized) {
        SerializationAssertions.assertEquals(original, deserialized);
    }

    @Test
    public void serializeBucketState_withSingleBandwidth_withoutState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[]{
                simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());

        testSerialization(bucketState);
    }

    @Test
    public void serializeBucketState_withSingleBandwidth_withState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[]{
                simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());

        bucketState.addTokens(bandwidths, 300);

        testSerialization(bucketState);
    }

    @Test
    public void serializeBucketState_withMultipleBandwidths_withState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[]{
                simple(10, ofSeconds(42)),
                classic(20, greedy(300, ofHours(2))),
                classic(400, intervallyAligned(1000, ofDays(2), Instant.now(), false))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());

        bucketState.addTokens(bandwidths, 42);

        testSerialization(bucketState);
    }
}
