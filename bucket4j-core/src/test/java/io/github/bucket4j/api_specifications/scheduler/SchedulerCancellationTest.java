package io.github.bucket4j.api_specifications.scheduler;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.SchedulingBucket;
import io.github.bucket4j.SimpleBucketListener;
import io.github.bucket4j.VerboseResult;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.mock.ProxyManagerMock;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(10)
class SchedulerCancellationTest {

    private static final long REFILL_NANOS = Duration.ofHours(1).toNanos();
    private static final BucketConfiguration CONFIGURATION = BucketConfiguration.builder()
        .addLimit(limit -> limit.capacity(1).refillGreedy(1, Duration.ofHours(1)).initialTokens(0))
        .build();

    private final RecordingScheduler scheduler = new RecordingScheduler();

    @AfterEach
    void shutdownScheduler() {
        scheduler.shutdownNow();
    }

    @ParameterizedTest
    @MethodSource("cases")
    void shouldCancelDelayedTaskWithoutRefundingTokens(TestCase testCase) {
        Fixture fixture = createBucket(testCase);
        CompletableFuture<?> result = testCase.consume(fixture.bucket(), scheduler);
        Future<?> task = (Future<?>) scheduler.getQueue().element();

        assertThat(result.cancel(true)).isTrue();

        assertThat(result.isCancelled()).isTrue();
        assertThat(task.isCancelled()).isTrue();
        assertThat(scheduler.getRemoveOnCancelPolicy()).isFalse();
        assertThat(fixture.availableTokens().getAsLong()).isEqualTo(-1);
        assertConsumedOnce(fixture.listener());
        scheduler.completion.run();
        assertThat(result.isCancelled()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("cases")
    void shouldRemoveCancelledTasksWhenRemoveOnCancelIsEnabled(TestCase testCase) {
        scheduler.setRemoveOnCancelPolicy(true);
        Fixture fixture = createBucket(testCase);
        List<CompletableFuture<?>> results = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            results.add(testCase.consume(fixture.bucket(), scheduler));
        }
        List<Runnable> tasks = new ArrayList<>(scheduler.getQueue());
        assertThat(tasks).hasSize(1_000);

        results.forEach(result -> assertThat(result.cancel(false)).isTrue());

        assertThat(tasks).allMatch(task -> ((Future<?>) task).isCancelled());
        assertThat(scheduler.getQueue()).isEmpty();
        assertThat(fixture.availableTokens().getAsLong()).isEqualTo(-1_000);
        assertThat(scheduler.isShutdown()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("asyncCases")
    void shouldNotScheduleAfterCancellationWhileWaitingForReservation(TestCase testCase) {
        Fixture fixture = createBucket(testCase);
        fixture.proxyManager().deferNextRequest = true;
        CompletableFuture<?> result = testCase.consume(fixture.bucket(), scheduler);

        assertThat(result.cancel(false)).isTrue();
        fixture.proxyManager().completeRequest.run();

        assertThat(result.isCancelled()).isTrue();
        assertThat(scheduler.getQueue()).isEmpty();
        assertThat(scheduler.completion).isNull();
        assertThat(fixture.availableTokens().getAsLong()).isEqualTo(-1);
        assertConsumedOnce(fixture.listener());
    }

    @ParameterizedTest
    @MethodSource("asyncCases")
    void shouldCancelTaskWhenCancelledBeforeScheduleReturns(TestCase testCase) {
        Fixture fixture = createBucket(testCase);
        fixture.proxyManager().deferNextRequest = true;
        CompletableFuture<?> result = testCase.consume(fixture.bucket(), scheduler);
        scheduler.beforeReturning = () -> assertThat(result.cancel(false)).isTrue();

        fixture.proxyManager().completeRequest.run();

        assertThat(result.isCancelled()).isTrue();
        assertThat(((Future<?>) scheduler.getQueue().element()).isCancelled()).isTrue();
        assertThat(fixture.availableTokens().getAsLong()).isEqualTo(-1);
        assertConsumedOnce(fixture.listener());
    }

    @ParameterizedTest
    @MethodSource("cases")
    void shouldCompleteNormallyWhenTaskRunsBeforeScheduleReturns(TestCase testCase) {
        Fixture fixture = createBucket(testCase);
        scheduler.beforeReturning = () -> scheduler.completion.run();

        CompletableFuture<?> result = testCase.consume(fixture.bucket(), scheduler);

        assertThat(result.isDone()).isTrue();
        assertSuccessfulResult(testCase, result);
        assertThat(result.cancel(false)).isFalse();
        assertConsumedOnce(fixture.listener());
    }

    @ParameterizedTest
    @MethodSource("cases")
    void shouldPreserveSchedulingFailure(TestCase testCase) {
        Fixture fixture = createBucket(testCase);
        scheduler.shutdown();

        CompletableFuture<?> result = testCase.consume(fixture.bucket(), scheduler);

        assertThatThrownBy(result::join).hasCauseInstanceOf(RejectedExecutionException.class);
        assertThat(fixture.availableTokens().getAsLong()).isEqualTo(-1);
        assertConsumedOnce(fixture.listener());
    }

    @ParameterizedTest
    @MethodSource("cases")
    void shouldCompleteOrCancelWhenCompletionRacesWithCancellation(TestCase testCase) throws Exception {
        Fixture fixture = createBucket(testCase);
        CompletableFuture<?> result = testCase.consume(fixture.bucket(), scheduler);
        Future<?> task = (Future<?>) scheduler.getQueue().element();
        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<Void> cancellation = CompletableFuture.runAsync(() -> {
            await(start);
            result.cancel(false);
        });
        CompletableFuture<Void> completion = CompletableFuture.runAsync(() -> {
            await(start);
            scheduler.completion.run();
        });

        start.countDown();
        CompletableFuture.allOf(cancellation, completion).get(5, TimeUnit.SECONDS);

        assertThat(result.isDone()).isTrue();
        if (result.isCancelled()) {
            assertThat(task.isCancelled()).isTrue();
        } else {
            assertSuccessfulResult(testCase, result);
        }
        assertThat(fixture.availableTokens().getAsLong()).isEqualTo(-1);
        assertConsumedOnce(fixture.listener());
    }

    private static void assertConsumedOnce(SimpleBucketListener listener) {
        assertThat(listener.getConsumed()).isEqualTo(1);
        assertThat(listener.getDelayedNanos()).isEqualTo(REFILL_NANOS);
        assertThat(listener.getRejected()).isZero();
        assertThat(listener.getInterrupted()).isZero();
    }

    private static void assertSuccessfulResult(TestCase testCase, CompletableFuture<?> result) {
        Object value = result.join();
        if (testCase.verbose()) {
            value = ((VerboseResult<?>) value).getValue();
        }
        assertThat(value).isEqualTo(testCase.tryConsume() ? Boolean.TRUE : null);
    }

    private static void await(CountDownLatch start) {
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Race did not start");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static Fixture createBucket(TestCase testCase) {
        TimeMeterMock clock = new TimeMeterMock();
        SimpleBucketListener listener = new SimpleBucketListener();
        if (testCase.async()) {
            DeferredProxyManager proxyManager = new DeferredProxyManager(clock);
            AsyncBucketProxy bucket = proxyManager.asAsync().builder()
                .withListener(listener).build(42, CONFIGURATION);
            assertThat(bucket.getAvailableTokens().join()).isZero();
            return new Fixture(bucket.asScheduler(), () -> bucket.getAvailableTokens().join(), listener, proxyManager);
        }
        Bucket bucket = Bucket.builder().addLimit(CONFIGURATION.getBandwidths()[0])
            .withCustomTimePrecision(clock).withListener(listener).build();
        return new Fixture(bucket.asScheduler(), bucket::getAvailableTokens, listener, null);
    }

    private static Stream<TestCase> cases() {
        List<TestCase> cases = new ArrayList<>();
        for (boolean async : new boolean[]{false, true}) {
            for (boolean verbose : new boolean[]{false, true}) {
                for (boolean tryConsume : new boolean[]{false, true}) {
                    cases.add(new TestCase(async, verbose, tryConsume));
                }
            }
        }
        return cases.stream();
    }

    private static Stream<TestCase> asyncCases() {
        return cases().filter(TestCase::async);
    }

    private record TestCase(boolean async, boolean verbose, boolean tryConsume) {
        CompletableFuture<?> consume(SchedulingBucket bucket, ScheduledExecutorService scheduler) {
            if (verbose) {
                return tryConsume ? bucket.asVerbose().tryConsume(1, Duration.ofDays(365), scheduler)
                    : bucket.asVerbose().consume(1, scheduler);
            }
            return tryConsume ? bucket.tryConsume(1, Duration.ofDays(365), scheduler)
                : bucket.consume(1, scheduler);
        }
    }

    private record Fixture(SchedulingBucket bucket, LongSupplier availableTokens,
                           SimpleBucketListener listener, DeferredProxyManager proxyManager) {
    }

    private static class RecordingScheduler extends ScheduledThreadPoolExecutor {
        private Runnable completion;
        private Runnable beforeReturning = () -> { };

        RecordingScheduler() {
            super(1);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            completion = command;
            ScheduledFuture<?> task = super.schedule(command, delay, unit);
            beforeReturning.run();
            return task;
        }
    }

    private static class DeferredProxyManager extends ProxyManagerMock<Integer> {
        private boolean deferNextRequest;
        private Runnable completeRequest;

        DeferredProxyManager(TimeMeterMock clock) {
            super(clock);
        }

        @Override
        public <T> CompletableFuture<CommandResult<T>> executeAsync(Integer key, Request<T> request) {
            if (!deferNextRequest) {
                return super.executeAsync(key, request);
            }
            deferNextRequest = false;
            CompletableFuture<CommandResult<T>> response = new CompletableFuture<>();
            completeRequest = () -> response.complete(super.execute(key, request));
            return response;
        }
    }
}
