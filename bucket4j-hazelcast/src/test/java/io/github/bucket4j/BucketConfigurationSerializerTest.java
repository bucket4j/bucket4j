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

public class BucketConfigurationSerializerTest extends SerializerTest<BucketConfiguration> {

    @Override
    protected StreamSerializer<BucketConfiguration> getSerializerUnderTest() {
        return this.bucketConfigurationSerializer;
    }

    @Override
    protected void runAssertions(BucketConfiguration original, BucketConfiguration deserialized) {
        SerializationAssertions.assertEquals(original, deserialized);
    }

    @Test
    public void serializeBucketConfiguration_withSingleBandwidth() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[]{
                simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));

        testSerialization(bucketConfiguration);
    }

    @Test
    public void serializeBucketConfiguration_withMultipleBandwidths() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[]{
                simple(10, ofSeconds(42)),
                classic(20, greedy(300, ofHours(2))),
                classic(400, intervallyAligned(1000, ofDays(2), Instant.now().plusNanos(1), true))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));

        testSerialization(bucketConfiguration);
    }
}
