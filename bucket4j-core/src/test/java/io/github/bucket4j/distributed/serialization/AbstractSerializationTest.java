
package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.remote.commands.ForceAddTokensCommand;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.remote.commands.*;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static io.github.bucket4j.Bandwidth.classic;
import static io.github.bucket4j.Bandwidth.simple;
import static io.github.bucket4j.Refill.*;
import static io.github.bucket4j.distributed.serialization.PrimitiveSerializationHandles.*;
import static java.time.Duration.*;
import static org.junit.Assert.assertTrue;

public abstract class AbstractSerializationTest {

    protected void testSerialization(Object object) {
        Object object2 = serializeAndDeserialize(object);
        assertTrue(ComparableByContent.equals(object, object2));
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
        BucketState bucketState = BucketState.createInitialState(bucketConfiguration, MathType.INTEGER_64_BITS, System.nanoTime());

        testSerialization(bucketState);
    }

    @Test
    public void serializeBucketState_withSingleBandwidth_withState() throws IOException {
        for (MathType mathType : MathType.values()) {
            Bandwidth[] bandwidths = new Bandwidth[] {
                    simple(10, ofSeconds(42))
            };
            BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
            BucketState bucketState = BucketState.createInitialState(bucketConfiguration, mathType, System.nanoTime());

            bucketState.addTokens(300);

            testSerialization(bucketState);
        }
    }

    @Test
    public void serializeBucketState_withMultipleBandwidths_withState() throws IOException {
        for (MathType mathType : MathType.values()) {
            Bandwidth[] bandwidths = new Bandwidth[]{
                    simple(10, ofSeconds(42)),
                    classic(20, greedy(300, ofHours(2))),
                    classic(400, intervallyAligned(1000, ofDays(2), Instant.now(), false))
            };
            BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
            BucketState bucketState = BucketState.createInitialState(bucketConfiguration, mathType, System.nanoTime());

            bucketState.addTokens(42);

            testSerialization(bucketState);
        }
    }


    @Test
    public void serializeGridBucketState_withSingleBandwidth_withoutState() throws IOException {
        for (MathType mathType : MathType.values()) {
            Bandwidth[] bandwidths = new Bandwidth[] {
                    simple(10, ofSeconds(42))
            };
            BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
            BucketState bucketState = BucketState.createInitialState(bucketConfiguration, mathType, System.nanoTime());
            RemoteBucketState gridBucketState = new RemoteBucketState(bucketState, new RemoteStat(14));

            testSerialization(gridBucketState);
        }
    }

    @Test
    public void serializeGridBucketState_withSingleBandwidth_withState() throws IOException {
        for (MathType mathType : MathType.values()) {
            Bandwidth[] bandwidths = new Bandwidth[] {
                    simple(10, ofSeconds(42))
            };
            BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
            BucketState bucketState = BucketState.createInitialState(bucketConfiguration, mathType, System.nanoTime());

            bucketState.addTokens(300);
            RemoteBucketState gridBucketState = new RemoteBucketState(bucketState, new RemoteStat(666));

            testSerialization(gridBucketState);
        }
    }

    @Test
    public void serializeGridBucketState_withMultipleBandwidths_withState() throws IOException {
        for (MathType mathType : MathType.values()) {
            Bandwidth[] bandwidths = new Bandwidth[] {
                    simple(10, ofSeconds(42)),
                    classic(20, greedy(300, ofHours(2))),
                    classic(400, intervallyAligned(1000, ofDays(2), Instant.now(), false))
            };
            BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
            BucketState bucketState = BucketState.createInitialState(bucketConfiguration, mathType, System.nanoTime());

            bucketState.addTokens(42);
            RemoteBucketState gridBucketState = new RemoteBucketState(bucketState, new RemoteStat(66));

            testSerialization(gridBucketState);
        }
    }

    @Test
    public void serializationOfCommandResults() throws IOException {
        // without payload
        testSerialization(CommandResult.bucketNotFound());
        testSerialization(CommandResult.unsupportedType(1000));
        testSerialization(CommandResult.usageOfUnsupportedApiException(10, 9));
        testSerialization(CommandResult.usageOfObsoleteApiException(1, 9));


        // with long payload
        testSerialization(CommandResult.success(42L, LONG_HANDLE));
        Bandwidth[] bandwidths = new Bandwidth[] {
            simple(10, ofSeconds(42))
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = BucketState.createInitialState(bucketConfiguration, MathType.INTEGER_64_BITS, System.nanoTime());

        // with complex payload
        EstimationProbe resultWithComplexPayload = EstimationProbe.canNotBeConsumed(10, 20);
        testSerialization(CommandResult.success(resultWithComplexPayload, EstimationProbe.SERIALIZATION_HANDLE));
        // without payload
        testSerialization(CommandResult.bucketNotFound());

        // with integer payload
        testSerialization(CommandResult.success(42L, LONG_HANDLE));

        // with complex payload
        testSerialization(CommandResult.success(EstimationProbe.canNotBeConsumed(10, 20), EstimationProbe.SERIALIZATION_HANDLE.getTypeId()));

        // estimation probes
        testSerialization(EstimationProbe.canNotBeConsumed(10, 20));
        testSerialization(EstimationProbe.canBeConsumed(10));

        // consumption probes
        testSerialization(ConsumptionProbe.rejected(10, 20, 43));
        testSerialization(ConsumptionProbe.consumed(10, 46));

        // estimation probes
        testSerialization(EstimationProbe.canNotBeConsumed(10, 20));
        testSerialization(EstimationProbe.canBeConsumed(10));

        // consumption probes
        testSerialization(ConsumptionProbe.rejected(10, 20, 66));
        testSerialization(ConsumptionProbe.consumed(10, 66));

        // multi result
        testSerialization(new MultiResult(Arrays.asList(
                CommandResult.NOTHING,
                CommandResult.FALSE,
                CommandResult.ZERO,
                CommandResult.success(resultWithComplexPayload, EstimationProbe.SERIALIZATION_HANDLE),
                CommandResult.bucketNotFound()
        )));
        // verbose results
        RemoteStat remoteStat = new RemoteStat(42);
        RemoteBucketState remoteBucketState = new RemoteBucketState(bucketState, remoteStat);
        testSerialization(new RemoteVerboseResult<>(323L, NULL_HANDLE.getTypeId(), null, remoteBucketState));
        testSerialization(new RemoteVerboseResult<>(323L, BOOLEAN_HANDLE.getTypeId(), true, remoteBucketState));
        testSerialization(new RemoteVerboseResult<>(323L, LONG_HANDLE.getTypeId(), 6666666L, remoteBucketState));
        testSerialization(new RemoteVerboseResult<>(323L, ConsumptionProbe.SERIALIZATION_HANDLE.getTypeId(), ConsumptionProbe.consumed(10, 32), remoteBucketState));
    }

    @Test
    public void serializationOfCommands() throws IOException {
        BucketConfiguration configuration = Bucket4j.configurationBuilder()
                .addLimit(simple(10, ofSeconds(1)))
                .build();

        testSerialization(new CreateInitialStateCommand(configuration));

        testSerialization(new CreateInitialStateAndExecuteCommand<>(configuration, new ConsumeAsMuchAsPossibleCommand(13)));

        testSerialization(
            new MultiCommand(
                Arrays.asList(
                    new AddTokensCommand(3),
                    new GetAvailableTokensCommand(),
                    new TryConsumeCommand(10)
                )
            )
        );

        testSerialization(new ReserveAndCalculateTimeToSleepCommand(10, 20));

        testSerialization(new AddTokensCommand(3));
        testSerialization(new ForceAddTokensCommand(666));

        testSerialization(new ConsumeAsMuchAsPossibleCommand(13));

        testSerialization(new GetAvailableTokensCommand());

        testSerialization(new CreateSnapshotCommand());

        testSerialization(new EstimateAbilityToConsumeCommand(3));

        testSerialization(new TryConsumeCommand(10));

        testSerialization(new TryConsumeAndReturnRemainingTokensCommand(11));

        for (TokensInheritanceStrategy tokensInheritanceStrategy : TokensInheritanceStrategy.values()) {
            testSerialization(new ReplaceConfigurationCommand(configuration, tokensInheritanceStrategy));
        }

        testSerialization(new GetConfigurationCommand());

        testSerialization(new ConsumeIgnoringRateLimitsCommand(100));

        testSerialization(new VerboseCommand<>(new ConsumeIgnoringRateLimitsCommand(100)));
        testSerialization(new VerboseCommand<>(new GetAvailableTokensCommand()));
        testSerialization(new VerboseCommand<>(new ReplaceConfigurationCommand(configuration, TokensInheritanceStrategy.AS_IS)));
        testSerialization(new SyncCommand(20, 10000000));
        testSerialization(new ResetCommand());

        testSerialization(new Request(new GetAvailableTokensCommand(), Versions.getLatest(), null));
        testSerialization(new Request(new GetAvailableTokensCommand(), Versions.getLatest(), 0L));
    }

}
