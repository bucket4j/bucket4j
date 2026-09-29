package io.github.bucket4j.api_specifications.scheduler;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.SchedulingBucket;
import io.github.bucket4j.SimpleBucketListener;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.ProxyManagerMock;
import io.github.bucket4j.mock.SchedulerMock;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS;
import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DisplayName("ScheduledBucket Specification")
class ScheduledBucketTest {

    private record TypeVerboseAsyncCase(BucketType type, boolean verbose, boolean async) {

        @Override
        public String toString() {
            return type + " verbose=" + verbose + " async=" + async;
        }
    }

    private record SleepAndReturnCase(int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, BucketConfiguration configuration) {

        @Override
        public String toString() {
            return "#" + n;
        }
    }

    private static Stream<TypeVerboseAsyncCase> typeVerboseAsyncCases() {
        List<TypeVerboseAsyncCase> cases = new ArrayList<>();
        for (BucketType type : BucketType.values()) {
            for (boolean verbose : new boolean[]{false, true}) {
                for (boolean async : new boolean[]{false, true}) {
                    cases.add(new TypeVerboseAsyncCase(type, verbose, async));
                }
            }
        }
        return cases.stream();
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

    private final TimeMeterMock clock = new TimeMeterMock();
    private final SimpleBucketListener listener = new SimpleBucketListener();
    private final SchedulerMock scheduler = new SchedulerMock(clock);

    @ParameterizedTest
    @MethodSource("typeVerboseAsyncCases")
    void testForAsyncDelayedConsume(TypeVerboseAsyncCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean verbose = testCase.verbose();
        boolean async = testCase.async();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        SchedulingBucket bucket = async ?
            type.createAsyncBucket(configuration, clock, listener).asScheduler() :
            type.createBucket(configuration, clock, listener).asScheduler();

        if (verbose) {
            bucket.asVerbose().consume(9, scheduler).get();
        } else {
            bucket.consume(9, scheduler).get();
        }
        assertThat(scheduler.getAcummulatedDelayNanos()).isEqualTo(0);

        if (verbose) {
            bucket.asVerbose().consume(2, scheduler).get();
        } else {
            bucket.consume(2, scheduler).get();
        }
        assertThat(scheduler.getAcummulatedDelayNanos()).isEqualTo(100_000_000);

        ExecutionException ex = catchThrowableOfType(() -> {
            if (verbose) {
                bucket.asVerbose().consume(Long.MAX_VALUE, scheduler).get();
            } else {
                bucket.consume(Long.MAX_VALUE, scheduler).get();
            }
        }, ExecutionException.class);
        assertThat(ex.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("typeVerboseAsyncCases")
    void testListenerForAsyncDelayedConsume(TypeVerboseAsyncCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean verbose = testCase.verbose();
        boolean async = testCase.async();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        SchedulingBucket bucket = async ?
            type.createAsyncBucket(configuration, clock, listener).asScheduler() :
            type.createBucket(configuration, clock, listener).asScheduler();

        if (verbose) {
            bucket.asVerbose().consume(9, scheduler);
        } else {
            bucket.consume(9, scheduler);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getDelayedNanos()).isEqualTo(0);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        if (verbose) {
            bucket.asVerbose().consume(2, scheduler);
        } else {
            bucket.consume(2, scheduler);
        }
        assertThat(listener.getConsumed()).isEqualTo(11);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getDelayedNanos()).isEqualTo(100_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(0);
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
            for (boolean limitAsDuration : new boolean[]{true, false}) {
                for (boolean verbose : new boolean[]{true, false}) {
                    TimeMeterMock meter = new TimeMeterMock(0);
                    AsyncBucketProxy bucket = type.createAsyncBucket(configuration, meter);
                    SchedulerMock scheduler = new SchedulerMock();
                    if (limitAsDuration) {
                        if (verbose) {
                            assertThat(bucket.asScheduler().asVerbose().tryConsume(toConsume, Duration.ofNanos(sleepLimit), scheduler).get().getValue()).isEqualTo(requiredResult);
                        } else {
                            assertThat(bucket.asScheduler().tryConsume(toConsume, Duration.ofNanos(sleepLimit), scheduler).get()).isEqualTo(requiredResult);
                        }
                    } else {
                        if (verbose) {
                            assertThat(bucket.asScheduler().asVerbose().tryConsume(toConsume, sleepLimit, scheduler).get().getValue()).isEqualTo(requiredResult);
                        } else {
                            assertThat(bucket.asScheduler().tryConsume(toConsume, sleepLimit, scheduler).get()).isEqualTo(requiredResult);
                        }
                    }
                    assertThat(scheduler.getAcummulatedDelayNanos()).isEqualTo(requiredSleep);
                }
            }
        }
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @ParameterizedTest
    @MethodSource("typeVerboseAsyncCases")
    void shouldCompleteFutureExceptionallyIfSchedulerFailedToScheduleTheTask(TypeVerboseAsyncCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean async = testCase.async();
        boolean verbose = testCase.verbose();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(1).refillGreedy(1, Duration.ofNanos(1)))
            .build();
        ProxyManager proxyManagerMock = new ProxyManagerMock(SYSTEM_MILLISECONDS);
        SchedulerMock schedulerMock = new SchedulerMock();

        SchedulingBucket bucket = async ?
            proxyManagerMock.asAsync().builder().withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION).build("66", () -> configuration).asScheduler() :
            proxyManagerMock.builder().withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION).build("66", () -> configuration).asScheduler();

        schedulerMock.setException(new RuntimeException());
        CompletableFuture future;
        if (verbose) {
            future = bucket.asVerbose().tryConsume(10, Duration.ofNanos(100000), schedulerMock);
        } else {
            future = bucket.tryConsume(10, Duration.ofNanos(100000), schedulerMock);
        }
        assertThat(future.isCompletedExceptionally()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("typeVerboseAsyncCases")
    void testListenerForAsyncScheduledTryConsume(TypeVerboseAsyncCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean async = testCase.async();
        boolean verbose = testCase.verbose();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        SchedulingBucket bucket = async ?
            type.createAsyncBucket(configuration, clock, listener).asScheduler() :
            type.createBucket(configuration, clock, listener).asScheduler();

        if (verbose) {
            bucket.asVerbose().tryConsume(9, Duration.ofSeconds(1).toNanos(), scheduler).get();
        } else {
            bucket.tryConsume(9, Duration.ofSeconds(1).toNanos(), scheduler).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getDelayedNanos()).isEqualTo(0);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        if (verbose) {
            bucket.asVerbose().tryConsume(1000, Duration.ofSeconds(1).toNanos(), scheduler).get();
        } else {
            bucket.tryConsume(1000, Duration.ofSeconds(1).toNanos(), scheduler).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(1000);
        assertThat(listener.getDelayedNanos()).isEqualTo(0);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        if (verbose) {
            bucket.asVerbose().tryConsume(2, Duration.ofSeconds(1).toNanos(), scheduler).get();
        } else {
            bucket.tryConsume(2, Duration.ofSeconds(1).toNanos(), scheduler).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(11);
        assertThat(listener.getRejected()).isEqualTo(1000);
        assertThat(listener.getDelayedNanos()).isEqualTo(100_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(0);
    }

}
