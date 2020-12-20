
package io.github.bucket4j.serialization;

import io.github.bucket4j.*;
import io.github.bucket4j.grid.*;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static io.github.bucket4j.Bandwidth.classic;
import static io.github.bucket4j.Bandwidth.simple;
import static io.github.bucket4j.Refill.*;
import static java.time.Duration.*;
import static org.junit.Assert.assertTrue;

public abstract class AbstractSerializationTest {

    protected void testSerialization(Object object) {
        Object object2 = serializeAndDeserialize(object);
        assertTrue(EqualityUtils.equals(object, object2));
    }

    protected abstract <T> T serializeAndDeserialize(T object);


    @Test
    public void serializeSimpleBandwidth() throws IOException {
        Bandwidth bandwidth = simple(10, ofSeconds(20));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithGreedyRefill() throws IOException {
        Bandwidth bandwidth = classic(20, greedy(100, Duration.ofSeconds(42)));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithIntervallyRefill() throws IOException {
        Bandwidth bandwidth = classic(30, intervally(200, Duration.ofSeconds(420)));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithIntervallyAlignedRefill() throws IOException {
        Bandwidth bandwidth = classic(40, intervallyAligned(300, Duration.ofSeconds(4200), Instant.now(), true));
        testSerialization(bandwidth);
    }

    @Test
    public void serializeBandwidthWithId() throws IOException {
        Bandwidth bandwidth = classic(40, intervallyAligned(300, Duration.ofSeconds(4200), Instant.now(), true))
                .withId("123");
        testSerialization(bandwidth);
    }


    @Test
    public void serializeBucketConfiguration_withSingleBandwidth() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[] {
                simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        testSerialization(bucketConfiguration);
    }

    @Test
    public void serializeBucketConfiguration_withMultipleBandwidths() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[] {
                simple(10, ofSeconds(42)),
                classic(20, greedy(300, ofHours(2))),
                classic(400, intervallyAligned(1000, ofDays(2), Instant.now().plusNanos(1), true))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        testSerialization(bucketConfiguration);
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


    @Test
    public void serializeGridBucketState_withSingleBandwidth_withoutState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[] {
                simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());
        GridBucketState gridBucketState = new GridBucketState(bucketConfiguration, bucketState);

        testSerialization(gridBucketState);
    }

    @Test
    public void serializeGridBucketState_withSingleBandwidth_withState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[] {
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
        Bandwidth[] bandwidths = new Bandwidth[] {
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

    @Test
    public void serializationOfCommandResults() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[] {
            simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = new BucketState(bucketConfiguration, System.nanoTime());

//        // without payload
//        testSerialization(CommandResult.bucketNotFound());
//
//        // with integer payload
//        testSerialization(CommandResult.success(42L));
//
//        // with complex payload
//        testSerialization(CommandResult.success(EstimationProbe.canNotBeConsumed(10, 20)));
//
//        // estimation probes
//        testSerialization(EstimationProbe.canNotBeConsumed(10, 20));
//        testSerialization(EstimationProbe.canBeConsumed(10));
//
//        // consumption probes
//        testSerialization(ConsumptionProbe.rejected(10, 20));
//        testSerialization(ConsumptionProbe.consumed(10));

        // verbose results
        testSerialization(new VerboseResult<>(323L, null, bucketConfiguration, bucketState));
        testSerialization(new VerboseResult<>(323L, true, bucketConfiguration, bucketState));
        testSerialization(new VerboseResult<>(323L, 6666666L, bucketConfiguration, bucketState));
        testSerialization(new VerboseResult<>(323L, ConsumptionProbe.consumed(10), bucketConfiguration, bucketState));
    }

    @Test
    public void serializationOfCommands() throws IOException {
        testSerialization(new ReserveAndCalculateTimeToSleepCommand(10, 20));

        testSerialization(new AddTokensCommand(3));

        testSerialization(new ConsumeAsMuchAsPossibleCommand(13));

        testSerialization(new GetAvailableTokensCommand());

        testSerialization(new CreateSnapshotCommand());

        testSerialization(new EstimateAbilityToConsumeCommand(3));

        testSerialization(new TryConsumeCommand(10));

        testSerialization(new TryConsumeAndReturnRemainingTokensCommand(11));

        BucketConfiguration configuration = Bucket4j.configurationBuilder()
                .addLimit(simple(10, ofSeconds(1)))
                .build();
        testSerialization(new ReplaceConfigurationCommand(configuration, TokensMigrationMode.AS_IS));

        testSerialization(new ConsumeIgnoringRateLimitsCommand(100));

        testSerialization(new VerboseCommand<>(new ConsumeIgnoringRateLimitsCommand(100)));
        testSerialization(new VerboseCommand<>(new GetAvailableTokensCommand()));
        testSerialization(new VerboseCommand<>(new ReplaceConfigurationCommand(configuration, TokensMigrationMode.PROPORTIONALLY)));
    }

}
