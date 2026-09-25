package io.github.bucket4j.api_specifications.regular;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.VerboseResult;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static io.github.bucket4j.util.PackageAccessor.getState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Consume Ignoring Limits Specification")
class ConsumeIgnoringLimitsTest {

    private record NotOverflownCase(
        int n,
        long tokensToConsume,
        long nanosIncrement,
        long remainedTokens,
        BucketConfiguration configuration
    ) {

        @Override
        public String toString() {
            return "n=" + n + ", tokensToConsume=" + tokensToConsume + ", nanosIncrement=" + nanosIncrement;
        }
    }

    private record OverflownCase(
        int n,
        long tokensToConsume,
        long nanosIncrement,
        long remainedTokens,
        long overflowNanos,
        BucketConfiguration configuration
    ) {

        @Override
        public String toString() {
            return "n=" + n + ", tokensToConsume=" + tokensToConsume + ", nanosIncrement=" + nanosIncrement;
        }
    }

    private record BucketTypeCase(BucketType type) {

        @Override
        public String toString() {
            return "type=" + type;
        }
    }

    static Stream<NotOverflownCase> caseWhenLimitsAreNotOverflownCases() {
        return Stream.of(
            new NotOverflownCase(1, 49, 50, 1,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new NotOverflownCase(2, 50, 50, 0,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new NotOverflownCase(3, 51, 120, 49,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new NotOverflownCase(4, 100, 101, 0,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build())
        );
    }

    static Stream<OverflownCase> caseWhenLimitsAreOverflownCases() {
        return Stream.of(
            new OverflownCase(1, 52, 50, -2, 2,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new OverflownCase(2, 50, 0, -50, 50,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new OverflownCase(3, 151, 120, -51, 51,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new OverflownCase(4, 400, 201, -300, 300,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build())
        );
    }

    static Stream<BucketTypeCase> bucketTypes() {
        return Stream.of(BucketType.values()).map(BucketTypeCase::new);
    }

    @ParameterizedTest
    @MethodSource("caseWhenLimitsAreNotOverflownCases")
    void caseWhenLimitsAreNotOverflown(NotOverflownCase testCase) throws Exception {
        for (BucketType type : BucketType.values()) {
            for (boolean sync : List.of(true, false)) {
                for (boolean verbose : List.of(false, true)) {
                    System.out.println("type=" + type + " sync=" + sync + " verbose=" + verbose);
                    TimeMeterMock timeMeter = new TimeMeterMock(0);
                    if (sync) {
                        Bucket bucket = type.createBucket(testCase.configuration(), timeMeter);
                        bucket.getAvailableTokens();
                        timeMeter.addTime(testCase.nanosIncrement());
                        if (!verbose) {
                            assertThat(bucket.consumeIgnoringRateLimits(testCase.tokensToConsume())).isEqualTo(0);
                        } else {
                            VerboseResult<Long> verboseResult = bucket.asVerbose().consumeIgnoringRateLimits(testCase.tokensToConsume());
                            assertThat(verboseResult.getValue()).isEqualTo(0L);
                            if (type.isLocal()) {
                                assertNotSame(verboseResult.getState(), getState(bucket));
                            }
                        }
                        assertThat(bucket.getAvailableTokens()).isEqualTo(testCase.remainedTokens());
                    } else {
                        AsyncBucketProxy asyncBucket = type.createAsyncBucket(testCase.configuration(), timeMeter);
                        asyncBucket.getAvailableTokens().get();
                        timeMeter.addTime(testCase.nanosIncrement());
                        if (!verbose) {
                            assertThat(asyncBucket.consumeIgnoringRateLimits(testCase.tokensToConsume()).get()).isEqualTo(0);
                        } else {
                            VerboseResult<Long> verboseResult = asyncBucket.asVerbose().consumeIgnoringRateLimits(testCase.tokensToConsume()).get();
                            assertThat(verboseResult.getValue()).isEqualTo(0L);
                        }
                        assertThat(asyncBucket.getAvailableTokens().get()).isEqualTo(testCase.remainedTokens());
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("caseWhenLimitsAreOverflownCases")
    void caseWhenLimitsAreOverflown(OverflownCase testCase) throws Exception {
        for (BucketType type : BucketType.values()) {
            for (boolean sync : List.of(true, false)) {
                for (boolean verbose : List.of(false, true)) {
                    System.out.println("type=" + type + " sync=" + sync + " verbose=" + verbose);
                    TimeMeterMock timeMeter = new TimeMeterMock(0);
                    if (sync) {
                        Bucket bucket = type.createBucket(testCase.configuration(), timeMeter);
                        bucket.getAvailableTokens();
                        timeMeter.addTime(testCase.nanosIncrement());
                        if (!verbose) {
                            assertThat(bucket.consumeIgnoringRateLimits(testCase.tokensToConsume())).isEqualTo(testCase.overflowNanos());
                        } else {
                            VerboseResult<Long> verboseResult = bucket.asVerbose().consumeIgnoringRateLimits(testCase.tokensToConsume());
                            assertThat(verboseResult.getValue()).isEqualTo(testCase.overflowNanos());
                            if (type.isLocal()) {
                                assertNotSame(verboseResult.getState(), getState(bucket));
                            }
                        }
                        assertThat(bucket.getAvailableTokens()).isEqualTo(testCase.remainedTokens());
                    } else {
                        AsyncBucketProxy asyncBucket = type.createAsyncBucket(testCase.configuration(), timeMeter);
                        asyncBucket.getAvailableTokens().get();
                        timeMeter.addTime(testCase.nanosIncrement());
                        if (!verbose) {
                            assertThat(asyncBucket.consumeIgnoringRateLimits(testCase.tokensToConsume()).get()).isEqualTo(testCase.overflowNanos());
                        } else {
                            VerboseResult<Long> verboseResult = asyncBucket.asVerbose().consumeIgnoringRateLimits(testCase.tokensToConsume()).get();
                            assertThat(verboseResult.getValue()).isEqualTo(testCase.overflowNanos());
                        }
                        assertThat(asyncBucket.getAvailableTokens().get()).isEqualTo(testCase.remainedTokens());
                    }
                }
            }
        }
    }

    @Test
    void reservationOverflowCase() throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(1, Duration.ofMinutes(1)).withInitialTokens(0))
            .build();
        long veryBigAmountOfTokensWhichCannotBeReserved = Long.MAX_VALUE / 2;
        for (BucketType type : BucketType.values()) {
            for (boolean sync : List.of(true, false)) {
                TimeMeterMock timeMeter = new TimeMeterMock(0);
                Bucket bucket = type.createBucket(configuration, timeMeter);
                if (sync) {
                    try {
                        bucket.consumeIgnoringRateLimits(veryBigAmountOfTokensWhichCannotBeReserved);
                        fail();
                    } catch (IllegalArgumentException e) {
                        assertThat(e.getMessage()).isEqualTo(BucketExceptions.reservationOverflow().getMessage());
                    }
                } else {
                    AsyncBucketProxy asyncBucket = type.createAsyncBucket(configuration, timeMeter);
                    try {
                        asyncBucket.consumeIgnoringRateLimits(veryBigAmountOfTokensWhichCannotBeReserved).get();
                        fail();
                    } catch (ExecutionException e) {
                        assertThat(e.getCause().getMessage()).isEqualTo(BucketExceptions.reservationOverflow().getMessage());
                    }
                }
            }
        }
    }

    @ParameterizedTest
    // https://github.com/bucket4j/bucket4j/issues/417
    @MethodSource("bucketTypes")
    void testConsumptionWhenAmountOfTokensBecameNegativeAfterConsumeIgnoringRateLimits(BucketTypeCase testCase) throws Exception {
        TimeMeterMock timeMeter = new TimeMeterMock(0);
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(it -> it.capacity(10).refillGreedy(5, Duration.ofSeconds(1)))
            .build();
        for (boolean sync : List.of(true, false)) {
            for (boolean verbose : List.of(false, true)) {
                if (sync) {
                    Bucket bucket = testCase.type().createBucket(configuration, timeMeter);
                    if (!verbose) {
                        bucket.consumeIgnoringRateLimits(15);
                        assertThat(bucket.tryConsume(1)).isFalse();
                        assertThat(bucket.tryConsumeAsMuchAsPossible()).isEqualTo(0);
                        assertThat(bucket.tryConsumeAsMuchAsPossible(2)).isEqualTo(0);
                        assertThat(bucket.estimateAbilityToConsume(2).canBeConsumed()).isFalse();
                        assertThat(bucket.tryConsumeAndReturnRemaining(2).isConsumed()).isFalse();
                    } else {
                        bucket.asVerbose().consumeIgnoringRateLimits(15);
                        assertThat(bucket.asVerbose().tryConsume(1).getValue()).isFalse();
                        assertThat(bucket.asVerbose().tryConsumeAsMuchAsPossible().getValue()).isEqualTo(0);
                        assertThat(bucket.asVerbose().tryConsumeAsMuchAsPossible(2).getValue()).isEqualTo(0);
                        assertThat(bucket.asVerbose().estimateAbilityToConsume(2).getValue().canBeConsumed()).isFalse();
                        assertThat(bucket.asVerbose().tryConsumeAndReturnRemaining(2).getValue().isConsumed()).isFalse();
                    }
                } else {
                    AsyncBucketProxy bucket = testCase.type().createAsyncBucket(configuration, timeMeter);
                    if (!verbose) {
                        bucket.consumeIgnoringRateLimits(15).get();
                        assertThat(bucket.tryConsume(1).get()).isFalse();
                        assertThat(bucket.tryConsumeAsMuchAsPossible().get()).isEqualTo(0);
                        assertThat(bucket.tryConsumeAsMuchAsPossible(2).get()).isEqualTo(0);
                        assertThat(bucket.estimateAbilityToConsume(2).get().canBeConsumed()).isFalse();
                        assertThat(bucket.tryConsumeAndReturnRemaining(2).get().isConsumed()).isFalse();
                    } else {
                        bucket.asVerbose().consumeIgnoringRateLimits(15).get();
                        assertThat(bucket.asVerbose().tryConsume(1).get().getValue()).isFalse();
                        assertThat(bucket.asVerbose().tryConsumeAsMuchAsPossible().get().getValue()).isEqualTo(0);
                        assertThat(bucket.asVerbose().tryConsumeAsMuchAsPossible(2).get().getValue()).isEqualTo(0);
                        assertThat(bucket.asVerbose().estimateAbilityToConsume(2).get().getValue().canBeConsumed()).isFalse();
                        assertThat(bucket.asVerbose().tryConsumeAndReturnRemaining(2).get().getValue().isConsumed()).isFalse();
                    }
                }
            }
        }
    }

}
