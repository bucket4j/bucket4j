package io.github.bucket4j.hazelcast.serialization;

import com.hazelcast.nio.serialization.StreamSerializer;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastSerializer;
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

public class GridBucketStateSerializerTest extends SerializerTest<GridBucketState> {

    @Override
    protected StreamSerializer<GridBucketState> getSerializerUnderTest() {
        return HazelcastSerializer.GRID_BUCKET_STATE_SERIALIZER;
    }

    @Override
    protected void runAssertions(GridBucketState original, GridBucketState deserialized) {
        BucketConfiguration originalConfiguration = original.getConfiguration();
        BucketConfiguration deserializedConfiguration = deserialized.getConfiguration();
        SerializationAssertions.assertEquals(originalConfiguration, deserializedConfiguration);

        BucketState originalState = original.getState();
        BucketState deserializedState = deserialized.getState();
        SerializationAssertions.assertEquals(originalState, deserializedState);
    }

    @Test
    public void test() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[]{
                simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());
        GridBucketState gridBucketState = new GridBucketState(bucketConfiguration, bucketState);

        testSerialization(gridBucketState);
    }

    @Test
    public void serializeGridBucketState_withSingleBandwidth_withoutState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[]{
                simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());
        GridBucketState gridBucketState = new GridBucketState(bucketConfiguration, bucketState);

        testSerialization(gridBucketState);
    }

    @Test
    public void serializeGridBucketState_withSingleBandwidth_withState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[]{
                simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());

        bucketState.addTokens(bandwidths, 300);
        GridBucketState gridBucketState = new GridBucketState(bucketConfiguration, bucketState);

        testSerialization(gridBucketState);
    }

    @Test
    public void serializeGridBucketState_withMultipleBandwidths_withState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[]{
                simple(10, ofSeconds(42)),
                classic(20, greedy(300, ofHours(2))),
                classic(400, intervallyAligned(1000, ofDays(2), Instant.now(), false))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());

        bucketState.addTokens(bandwidths, 42);
        GridBucketState gridBucketState = new GridBucketState(bucketConfiguration, bucketState);

        testSerialization(gridBucketState);
    }
}
