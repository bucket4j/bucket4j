
package io.github.bucket4j.distributed.serialization;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.remote.commands.*;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.local.LockFreeBucket;
import io.github.bucket4j.local.ConcurrencyStrategy;
import io.github.bucket4j.local.ReentrantLockProtectedBucket;
import io.github.bucket4j.local.SynchronizedBucket;
import io.github.bucket4j.local.ThreadUnsafeBucket;
import io.github.bucket4j.util.ComparableByContent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static io.github.bucket4j.distributed.serialization.PrimitiveSerializationHandles.*;
import static java.time.Duration.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractSerializationTest {

    protected void testSerialization(Object object) {
        for (Scope scope : Scope.values()) {
            Object object2 = serializeAndDeserialize(object, scope);
            assertTrue(ComparableByContent.equals(object, object2));
        }
    }

    protected abstract <T> T serializeAndDeserialize(T object, Scope scope);


    @Test
    public void serializeSimpleBandwidth() throws IOException {
        Bandwidth bandwidth = Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(20)).build();
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithGreedyRefill() throws IOException {
        Bandwidth bandwidth = Bandwidth.builder().capacity(20).refillGreedy(100, Duration.ofSeconds(42)).build();
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithIntervallyRefill() throws IOException {
        Bandwidth bandwidth = Bandwidth.builder().capacity(30).refillIntervally(200, Duration.ofSeconds(420)).build();
        testSerialization(bandwidth);
    }

    @Test
    public void serializeClassicBandwidthWithIntervallyAlignedRefill() throws IOException {
        Bandwidth limit = Bandwidth.builder()
            .capacity(40)
            .refillIntervallyAlignedWithAdaptiveInitialTokens(300, Duration.ofSeconds(4200), Instant.now())
            .build();
        testSerialization(limit);
    }

    @Test
    public void serializeBandwidthWithId() throws IOException {
        Bandwidth bandwidth = Bandwidth.builder()
            .capacity(40)
            .refillIntervallyAlignedWithAdaptiveInitialTokens(300, Duration.ofSeconds(4200), Instant.now())
            .id("123")
            .build();
        testSerialization(bandwidth);
    }

    @Test
    public void serializeBucketConfiguration_withSingleBandwidth() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[] {
            Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(42)).build(),
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        testSerialization(bucketConfiguration);
    }

    @Test
    public void serializeBucketConfiguration_withMultipleBandwidths() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[] {
            Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(42)).build(),
            Bandwidth.builder().capacity(20).refillGreedy(300, ofHours(2)).build(),
            Bandwidth.builder().capacity(400).refillIntervallyAlignedWithAdaptiveInitialTokens (1000, ofDays(2), Instant.now()).build()
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        testSerialization(bucketConfiguration);
    }

    @Test
    public void serializeBucketState_withSingleBandwidth_withoutState() throws IOException {
        Bandwidth[] bandwidths = new Bandwidth[] {
            Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(42)).build(),
        };
        BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
        BucketState bucketState = BucketState.createInitialState(bucketConfiguration, MathType.INTEGER_64_BITS, System.nanoTime());

        testSerialization(bucketState);
    }

    @Test
    public void serializeBucketState_withSingleBandwidth_withState() throws IOException {
        for (MathType mathType : MathType.values()) {
            Bandwidth[] bandwidths = new Bandwidth[] {
                Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(42)).build()
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
            Bandwidth[] bandwidths = new Bandwidth[] {
                Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(42)).build(),
                Bandwidth.builder().capacity(20).refillGreedy(300, ofHours(2)).build(),
                Bandwidth.builder().capacity(400).refillIntervallyAlignedWithAdaptiveInitialTokens (1000, ofDays(2), Instant.now()).build()
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
                Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(42)).build()
            };
            BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
            BucketState bucketState = BucketState.createInitialState(bucketConfiguration, mathType, System.nanoTime());

            testSerialization(new RemoteBucketState(bucketState, new RemoteStat(14), null));
            testSerialization(new RemoteBucketState(bucketState, new RemoteStat(14), 0L));
        }
    }

    @Test
    public void serializeGridBucketState_withSingleBandwidth_withState() throws IOException {
        for (MathType mathType : MathType.values()) {
            Bandwidth[] bandwidths = new Bandwidth[] {
                Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(42)).build()
            };
            BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
            BucketState bucketState = BucketState.createInitialState(bucketConfiguration, mathType, System.nanoTime());

            bucketState.addTokens(300);


            testSerialization(new RemoteBucketState(bucketState, new RemoteStat(666), null));
            testSerialization(new RemoteBucketState(bucketState, new RemoteStat(666), 1L));
        }
    }

    @Test
    public void serializeGridBucketState_withMultipleBandwidths_withState() throws IOException {
        for (MathType mathType : MathType.values()) {
            Bandwidth[] bandwidths = new Bandwidth[] {
                    Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(42)).build(),
                    Bandwidth.builder().capacity(20).refillGreedy(300, ofHours(2)).build(),
                    Bandwidth.builder().capacity(400).refillIntervallyAlignedWithAdaptiveInitialTokens (1000, ofDays(2), Instant.now()).build()
            };
            BucketConfiguration bucketConfiguration = new BucketConfiguration(Arrays.asList(bandwidths));
            BucketState bucketState = BucketState.createInitialState(bucketConfiguration, mathType, System.nanoTime());

            bucketState.addTokens(42);

            testSerialization(new RemoteBucketState(bucketState, new RemoteStat(66), null));
            testSerialization(new RemoteBucketState(bucketState, new RemoteStat(66), 1L));
        }
    }

    @Test
    public void serializationOfCommandResults() throws IOException {
        // without payload
        testSerialization(CommandResult.bucketNotFound());
        testSerialization(CommandResult.unsupportedType(1000));
        testSerialization(CommandResult.usageOfUnsupportedApiException(10, 9));
        testSerialization(CommandResult.usageOfObsoleteApiException(1, 9));
        testSerialization(CommandResult.unsupportedNamedType("something"));
        testSerialization(CommandResult.configurationNeedToBeReplaced());


        // with long payload
        testSerialization(CommandResult.success(42L, LONG_HANDLE));
        Bandwidth[] bandwidths = new Bandwidth[] {
            Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(42)).build(),
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
        RemoteBucketState remoteBucketState = new RemoteBucketState(bucketState, remoteStat, null);
        testSerialization(new RemoteVerboseResult<>(323L, NULL_HANDLE.getTypeId(), null, remoteBucketState));
        testSerialization(new RemoteVerboseResult<>(323L, BOOLEAN_HANDLE.getTypeId(), true, remoteBucketState));
        testSerialization(new RemoteVerboseResult<>(323L, LONG_HANDLE.getTypeId(), 6666666L, remoteBucketState));
        testSerialization(new RemoteVerboseResult<>(323L, ConsumptionProbe.SERIALIZATION_HANDLE.getTypeId(), ConsumptionProbe.consumed(10, 32), remoteBucketState));
    }

    @Test
    public void serializationOfCommands() throws IOException {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, ofSeconds(1)).build())
                .build();

        testSerialization(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, new ConsumeAsMuchAsPossibleCommand(13), 1, TokensInheritanceStrategy.AS_IS));
        testSerialization(new CreateInitialStateAndExecuteCommand<>(configuration, new ConsumeAsMuchAsPossibleCommand(13)));
        testSerialization(new CheckConfigurationVersionAndExecuteCommand<>(new ConsumeAsMuchAsPossibleCommand(13), 1));

        testSerialization(
            new MultiCommand(
                Arrays.asList(
                    new AddTokensCommand(3),
                    new GetAvailableTokensCommand(),
                    TryConsumeCommand.create(10)
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

        testSerialization(TryConsumeCommand.create(10));

        testSerialization(new TryConsumeAndReturnRemainingTokensCommand(11));

        for (TokensInheritanceStrategy tokensInheritanceStrategy : TokensInheritanceStrategy.values()) {
            testSerialization(new ReplaceConfigurationCommand(configuration, tokensInheritanceStrategy));
        }

        testSerialization(new GetConfigurationCommand());

        testSerialization(new ConsumeIgnoringRateLimitsCommand(100));

        testSerialization(VerboseCommand.from(new ConsumeIgnoringRateLimitsCommand(100)));
        testSerialization(VerboseCommand.from(new GetAvailableTokensCommand()));
        testSerialization(VerboseCommand.from(new ReplaceConfigurationCommand(configuration, TokensInheritanceStrategy.AS_IS)));
        testSerialization(new SyncCommand(20, 10000000));
        testSerialization(new ResetCommand());

        testSerialization(new Request<>(new GetAvailableTokensCommand(), Versions.getLatest(), null, null));
        testSerialization(new Request<>(new GetAvailableTokensCommand(), Versions.getLatest(), 0L, null));
        testSerialization(new Request<>(new GetAvailableTokensCommand(), Versions.getLatest(), System.currentTimeMillis(), null));
        testSerialization(new Request<>(new GetAvailableTokensCommand(), Versions.getLatest(), null, ExpirationAfterWriteStrategy.none()));
    }

    @Test
    public void serializationOfBuckets() throws IOException {
        LockFreeBucket lockFreeBucket = (LockFreeBucket) Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(1).refillGreedy(1, Duration.ofSeconds(1)).build())
                .build();
        testSerialization(lockFreeBucket);

        ReentrantLockProtectedBucket reentrantLockProtectedBucket = (ReentrantLockProtectedBucket) Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(1).refillGreedy(1, Duration.ofSeconds(1)).build())
            .withSynchronizationStrategy(ConcurrencyStrategy.REENTRANT_LOCK_PROTECTED)
            .build();
        testSerialization(reentrantLockProtectedBucket);

        SynchronizedBucket synchronizedBucket = (SynchronizedBucket) Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(1).refillGreedy(1, Duration.ofSeconds(1)).build())
                .withSynchronizationStrategy(ConcurrencyStrategy.SYNCHRONIZED)
                .build();
        testSerialization(synchronizedBucket);

        ThreadUnsafeBucket unsafeBucket = (ThreadUnsafeBucket) Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(1).refillGreedy(1, Duration.ofSeconds(1)).build())
                .withSynchronizationStrategy(ConcurrencyStrategy.UNSAFE)
                .build();
        testSerialization(unsafeBucket);
    }

    @Test
    public void serializationOfExpirationStrategies() throws IOException {
        testSerialization(ExpirationAfterWriteStrategy.none());
        testSerialization(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofSeconds(60)));
        testSerialization(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(15)));
    }

}
