package io.github.bucket4j.api_specifications.blocking;

import io.github.bucket4j.BlockingStrategy;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.SimpleBucketListener;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.mock.BlockingStrategyMock;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BlockingTryConsume Specification")
class BlockingTryConsumeTest {

    private record SleepCase(int n, long requiredSleep, long toConsume, BucketConfiguration configuration) {

        @Override
        public String toString() {
            return "#" + n;
        }
    }

    private record SleepAndReturnCase(int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, BucketConfiguration configuration) {

        @Override
        public String toString() {
            return "#" + n;
        }
    }

    private record InterruptedCase(BucketType type, boolean verbose, TimeMeter meter) {

        @Override
        public String toString() {
            return type + " verbose=" + verbose + " meter=" + meter;
        }
    }

    private record TypeVerboseCase(BucketType type, boolean verbose) {

        @Override
        public String toString() {
            return type + " verbose=" + verbose;
        }
    }

    private static Stream<SleepCase> sleepCases() {
        return Stream.of(
            new SleepCase(1, 10, 1, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0)).build()),
            new SleepCase(2, 0, 1, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)).build()),
            new SleepCase(3, 9990, 1000, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)).build())
        );
    }

    private static Stream<SleepAndReturnCase> sleepAndReturnCases() {
        return Stream.of(
            new SleepAndReturnCase(1, 10, true, 1, 11, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0)).build()),
            new SleepAndReturnCase(2, 10, true, 1, 11, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0)).build()),
            new SleepAndReturnCase(3, 0, true, 1, 11, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)).build()),
            new SleepAndReturnCase(4, 0, false, 1000, 11, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)).build()),
            new SleepAndReturnCase(5, 40, true, 5, 40, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)).build()),
            new SleepAndReturnCase(6, 40, true, 5, 41, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)).build()),
            new SleepAndReturnCase(6, 0, false, 5, 39, BucketConfiguration.builder().addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)).build())
        );
    }

    private static Stream<InterruptedCase> interruptedCases() {
        List<InterruptedCase> cases = new ArrayList<>();
        for (BucketType type : BucketType.values()) {
            for (boolean verbose : new boolean[]{false, true}) {
                for (TimeMeter meter : new TimeMeter[]{SYSTEM_MILLISECONDS, TimeMeter.SYSTEM_NANOTIME}) {
                    cases.add(new InterruptedCase(type, verbose, meter));
                }
            }
        }
        return cases.stream();
    }

    private static Stream<TypeVerboseCase> typeVerboseCases() {
        List<TypeVerboseCase> cases = new ArrayList<>();
        for (BucketType type : BucketType.values()) {
            for (boolean verbose : new boolean[]{false, true}) {
                cases.add(new TypeVerboseCase(type, verbose));
            }
        }
        return cases.stream();
    }

    private static class ParkingTrackingListener extends SimpleBucketListener {
        private long beforeParkingNanos;

        @Override
        public void beforeParking(long nanos) {
            beforeParkingNanos += nanos;
        }
    }

    private final TimeMeterMock clock = new TimeMeterMock();
    private final BlockingStrategyMock blocker = new BlockingStrategyMock(clock);

    private final ParkingTrackingListener listener = new ParkingTrackingListener();

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @ParameterizedTest
    @MethodSource("sleepCases")
    void shouldSleepWhenTryingToConsumeTokensFromBucket(SleepCase testCase) throws Exception {
        long requiredSleep = testCase.requiredSleep();
        long toConsume = testCase.toConsume();
        BucketConfiguration configuration = testCase.configuration();

        for (BucketType type : BucketType.values()) {
            for (boolean uniterruptible : new boolean[]{true, false}) {
                for (boolean limitAsDuration : new boolean[]{true, false}) {
                    for (boolean verbose : new boolean[]{true, false}) {
                        TimeMeterMock meter = new TimeMeterMock(0);
                        Bucket bucket = type.createBucket(configuration, meter);
                        BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter);
                        if (uniterruptible) {
                            if (limitAsDuration) {
                                if (verbose) {
                                    bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(toConsume, Duration.ofHours(1), sleepStrategy);
                                } else {
                                    bucket.asBlocking().tryConsumeUninterruptibly(toConsume, Duration.ofHours(1), sleepStrategy);
                                }
                            } else {
                                if (verbose) {
                                    bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy);
                                } else {
                                    bucket.asBlocking().tryConsumeUninterruptibly(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy);
                                }
                            }
                        } else {
                            if (limitAsDuration) {
                                if (verbose) {
                                    bucket.asBlocking().asVerbose().tryConsume(toConsume, Duration.ofHours(1), sleepStrategy);
                                } else {
                                    bucket.asBlocking().tryConsume(toConsume, Duration.ofHours(1), sleepStrategy);
                                }
                            } else {
                                if (verbose) {
                                    bucket.asBlocking().asVerbose().tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy);
                                } else {
                                    bucket.asBlocking().tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy);
                                }
                            }
                        }
                        assertThat(sleepStrategy.getParkedNanos()).isEqualTo(requiredSleep);
                    }
                }
            }
        }
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @ParameterizedTest
    @MethodSource("sleepAndReturnCases")
    void shouldSleepAndReturnRequiredResultWhenTryingToSynchronousConsume(SleepAndReturnCase testCase) throws Exception {
        long requiredSleep = testCase.requiredSleep();
        boolean requiredResult = testCase.requiredResult();
        long toConsume = testCase.toConsume();
        long sleepLimit = testCase.sleepLimit();
        BucketConfiguration configuration = testCase.configuration();

        for (BucketType type : BucketType.values()) {
            for (boolean uniterruptible : new boolean[]{true, false}) {
                for (boolean limitAsDuration : new boolean[]{true, false}) {
                    for (boolean verbose : new boolean[]{true, false}) {
                        TimeMeterMock meter = new TimeMeterMock(0);
                        Bucket bucket = type.createBucket(configuration, meter);
                        BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter);
                        if (uniterruptible) {
                            if (limitAsDuration) {
                                if (verbose) {
                                    assertThat(bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy).getValue()).isEqualTo(requiredResult);
                                } else {
                                    assertThat(bucket.asBlocking().tryConsumeUninterruptibly(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy)).isEqualTo(requiredResult);
                                }
                            } else {
                                if (verbose) {
                                    assertThat(bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(toConsume, sleepLimit, sleepStrategy).getValue()).isEqualTo(requiredResult);
                                } else {
                                    assertThat(bucket.asBlocking().tryConsumeUninterruptibly(toConsume, sleepLimit, sleepStrategy)).isEqualTo(requiredResult);
                                }
                            }
                        } else {
                            if (limitAsDuration) {
                                if (verbose) {
                                    assertThat(bucket.asBlocking().asVerbose().tryConsume(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy).getValue()).isEqualTo(requiredResult);
                                } else {
                                    assertThat(bucket.asBlocking().tryConsume(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy)).isEqualTo(requiredResult);
                                }
                            } else {
                                if (verbose) {
                                    assertThat(bucket.asBlocking().asVerbose().tryConsume(toConsume, sleepLimit, sleepStrategy).getValue()).isEqualTo(requiredResult);
                                } else {
                                    assertThat(bucket.asBlocking().tryConsume(toConsume, sleepLimit, sleepStrategy)).isEqualTo(requiredResult);
                                }
                            }
                        }
                        assertThat(sleepStrategy.getParkedNanos()).isEqualTo(requiredSleep);
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @MethodSource("interruptedCases")
    void shouldThrowInterruptedExceptionWhenThreadInterruptedDuringWaitingForTokenRefill(InterruptedCase testCase) throws Exception {
        BucketType type = testCase.type();

        BucketConfiguration config = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(1).refillGreedy(1, Duration.ofMinutes(1)).initialTokens(0))
            .build();
        Bucket bucket = type.createBucket(config, testCase.meter());

        Thread.currentThread().interrupt();
        InterruptedException thrown1 = null;
        try {
            bucket.asBlocking().tryConsume(1, TimeUnit.HOURS.toNanos(1000), BlockingStrategy.PARKING);
        } catch (InterruptedException e) {
            thrown1 = e;
        }
        assertThat(thrown1).isNotNull();

        InterruptedException thrown2 = null;
        Thread.currentThread().interrupt();
        try {
            bucket.asBlocking().tryConsume(1, TimeUnit.HOURS.toNanos(1), BlockingStrategy.PARKING);
        } catch (InterruptedException e) {
            thrown2 = e;
        }
        assertThat(thrown2).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("typeVerboseCases")
    void testListenerForBlockingTryConsume(TypeVerboseCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean verbose = testCase.verbose();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        Bucket bucket = type.createBucket(configuration, clock, listener);

        if (verbose) {
            bucket.asBlocking().asVerbose().tryConsume(9, Duration.ofSeconds(1), blocker);
        } else {
            bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getParkedNanos()).isEqualTo(0);
        assertThat(listener.beforeParkingNanos).isEqualTo(0);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        if (verbose) {
            bucket.asBlocking().asVerbose().tryConsume(1000, Duration.ofSeconds(1), blocker);
        } else {
            bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(1000);
        assertThat(listener.getParkedNanos()).isEqualTo(0);
        assertThat(listener.beforeParkingNanos).isEqualTo(0);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        if (verbose) {
            bucket.asBlocking().asVerbose().tryConsume(2, Duration.ofSeconds(1), blocker);
        } else {
            bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(11);
        assertThat(listener.getRejected()).isEqualTo(1000);
        assertThat(listener.getParkedNanos()).isEqualTo(100_000_000);
        assertThat(listener.beforeParkingNanos).isEqualTo(100_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> {
            if (verbose) {
                bucket.asBlocking().asVerbose().tryConsume(1, Duration.ofSeconds(1), blocker);
            } else {
                bucket.asBlocking().tryConsume(1, Duration.ofSeconds(1), blocker);
            }
        }).isInstanceOf(InterruptedException.class);
        assertThat(listener.getConsumed()).isEqualTo(12);
        assertThat(listener.getRejected()).isEqualTo(1000);
        assertThat(listener.getParkedNanos()).isEqualTo(100_000_000);
        assertThat(listener.beforeParkingNanos).isEqualTo(200_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("typeVerboseCases")
    void testListenerForBlockingTryConsumeUninterruptibly(TypeVerboseCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean verbose = testCase.verbose();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        Bucket bucket = type.createBucket(configuration, clock, listener);

        if (verbose) {
            bucket.asBlocking().asVerbose().tryConsume(9, Duration.ofSeconds(1), blocker);
        } else {
            bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getParkedNanos()).isEqualTo(0);
        assertThat(listener.beforeParkingNanos).isEqualTo(0);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        if (verbose) {
            bucket.asBlocking().asVerbose().tryConsume(1000, Duration.ofSeconds(1), blocker);
        } else {
            bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(1000);
        assertThat(listener.getParkedNanos()).isEqualTo(0);
        assertThat(listener.beforeParkingNanos).isEqualTo(0);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        if (verbose) {
            bucket.asBlocking().asVerbose().tryConsume(2, Duration.ofSeconds(1), blocker);
        } else {
            bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(11);
        assertThat(listener.getRejected()).isEqualTo(1000);
        assertThat(listener.getParkedNanos()).isEqualTo(100_000_000);
        assertThat(listener.beforeParkingNanos).isEqualTo(100_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        Thread.currentThread().interrupt();
        if (verbose) {
            bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(1, Duration.ofSeconds(1), blocker);
        } else {
            bucket.asBlocking().tryConsumeUninterruptibly(1, Duration.ofSeconds(1), blocker);
        }
        assertThat(Thread.interrupted()).isTrue();
        assertThat(listener.getConsumed()).isEqualTo(12);
        assertThat(listener.getRejected()).isEqualTo(1000);
        assertThat(listener.getParkedNanos()).isEqualTo(200_000_000);
        assertThat(listener.beforeParkingNanos).isEqualTo(200_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(0);
    }

}
