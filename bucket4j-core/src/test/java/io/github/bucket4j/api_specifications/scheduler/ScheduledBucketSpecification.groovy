package io.github.bucket4j.api_specifications.scheduler

import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.SchedulingBucket
import io.github.bucket4j.SimpleBucketListener
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.SchedulerMock
import io.github.bucket4j.mock.TimeMeterMock
import io.github.bucket4j.util.PipeGenerator
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class ScheduledBucketSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    SimpleBucketListener listener = new SimpleBucketListener()
    SchedulerMock scheduler = new SchedulerMock(clock)

    @Unroll
    def "#type test for async delayed consume verbose=#verbose async=#async"(BucketType type, boolean verbose, boolean async) {
        setup:
            def configuration = BucketConfiguration.builder()
                .addLimit(limit -> {limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
        SchedulingBucket bucket = async && type.isAsyncModeSupported()?
                type.createAsyncBucket(configuration, clock, listener).asScheduler() :
                type.createBucket(configuration, clock, listener).asScheduler()

        when:
            if (verbose) {
                bucket.asVerbose().consume(9, scheduler).get()
            } else {
                bucket.consume(9, scheduler).get()
            }
        then:
            scheduler.acummulatedDelayNanos == 0

        when:
            if (verbose) {
                bucket.asVerbose().consume(2, scheduler).get()
            } else {
                bucket.consume(2, scheduler).get()
            }
        then:
            scheduler.acummulatedDelayNanos == 100_000_000
        when:
            if (verbose) {
                bucket.asVerbose().consume(Long.MAX_VALUE, scheduler).get()
            } else {
                bucket.consume(Long.MAX_VALUE, scheduler).get()
            }
        then:
            ExecutionException ex = thrown(ExecutionException)
            ex.getCause().class == IllegalArgumentException

        where:
            [type, verbose, async] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true], [false, true])
    }

    @Unroll
    def "#type test listener for async delayed consume verbose=#verbose async=#async"(BucketType type, boolean verbose, boolean async) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> {limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            SchedulingBucket bucket = async && type.isAsyncModeSupported() ?
                    type.createAsyncBucket(configuration, clock, listener).asScheduler() :
                    type.createBucket(configuration, clock, listener).asScheduler()

        when:
            if (verbose) {
                bucket.asVerbose().consume(9, scheduler)
            } else {
                bucket.consume(9, scheduler)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            if (verbose) {
                bucket.asVerbose().consume(2, scheduler)
            } else {
                bucket.consume(2, scheduler)
            }
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 0
            listener.getDelayedNanos() == 100_000_000
            listener.getInterrupted() == 0

        where:
            [type, verbose, async] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true], [false, true])
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to synchronous consume #toConsume tokens with limit #sleepLimit from Bucket #configuration"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean limitAsDuration: [true, false]) {
                for (boolean verbose: [true, false]) {
                    for (boolean async:[true, false]) {
                        TimeMeterMock meter = new TimeMeterMock(0)
                        SchedulingBucket schedulingBucket = async && type.asyncModeSupported ?
                                type.createAsyncBucket(configuration, meter).asScheduler() :
                                type.createBucket(configuration, meter).asScheduler()
                        SchedulerMock scheduler = new SchedulerMock()
                        if (limitAsDuration) {
                            if (verbose) {
                                assert schedulingBucket.asVerbose().tryConsume(toConsume, Duration.ofNanos(sleepLimit), scheduler).get().value == requiredResult
                            } else {
                                assert schedulingBucket.tryConsume(toConsume, Duration.ofNanos(sleepLimit), scheduler).get() == requiredResult
                            }
                        } else {
                            if (verbose) {
                                assert schedulingBucket.asVerbose().tryConsume(toConsume, sleepLimit, scheduler).get().value == requiredResult
                            } else {
                                assert schedulingBucket.tryConsume(toConsume, sleepLimit, scheduler).get() == requiredResult
                            }
                        }
                        assert scheduler.acummulatedDelayNanos == requiredSleep
                    }
                }
            }
        }
        where:
        n | requiredSleep | requiredResult | toConsume | sleepLimit |  configuration

        1 |      10       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0)}).build()
        2 |      10       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0)}).build()
        3 |       0       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)}).build()
        4 |       0       |     false      |   1000    |     11     |  BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)}).build()
        5 |      40       |     true       |     5     |     40     |  BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)}).build()
        6 |      40       |     true       |     5     |     41     |  BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)}).build()
        6 |       0       |     false      |     5     |     39     |  BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)}).build()
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    def "#type should complete future exceptionally if scheduler failed to schedule the task async=#async verbose=#verbose"(BucketType type, boolean async, boolean verbose) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(1).refillGreedy(1, Duration.ofNanos(1))})
                .build()

            SchedulerMock schedulerMock = new SchedulerMock()
            SchedulingBucket bucket = async && type.asyncModeSupported ?
                type.createAsyncBucket(configuration, clock).asScheduler() :
                type.createBucket(configuration, clock).asScheduler()

        when:
            schedulerMock.setException(new RuntimeException())
            CompletableFuture future
            if (verbose) {
                future = bucket.asVerbose().tryConsume(10, Duration.ofNanos(100000), schedulerMock)
            } else {
                future = bucket.tryConsume(10, Duration.ofNanos(100000), schedulerMock)
            }
        then:
            future.isCompletedExceptionally()
        where:
            [type, verbose, async] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true], [false, true])
    }

    @Unroll
    def "#type test listener for async scheduled tryConsume async=#async verbose=#verbose"(BucketType type, boolean async, boolean verbose) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            SchedulingBucket bucket = async && type.isAsyncModeSupported() ?
                type.createAsyncBucket(configuration, clock, listener).asScheduler() :
                type.createBucket(configuration, clock, listener).asScheduler()

        when:
            if (verbose) {
                bucket.asVerbose().tryConsume(9, Duration.ofSeconds(1).toNanos(), scheduler).get()
            } else {
                bucket.tryConsume(9, Duration.ofSeconds(1).toNanos(), scheduler).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            if (verbose) {
                bucket.asVerbose().tryConsume(1000, Duration.ofSeconds(1).toNanos(), scheduler).get()
            } else {
                bucket.tryConsume(1000, Duration.ofSeconds(1).toNanos(), scheduler).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            if (verbose) {
                bucket.asVerbose().tryConsume(2, Duration.ofSeconds(1).toNanos(), scheduler).get()
            } else {
                bucket.tryConsume(2, Duration.ofSeconds(1).toNanos(), scheduler).get()
            }
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getDelayedNanos() == 100_000_000
            listener.getInterrupted() == 0

        where:
            [type, verbose, async] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true], [false, true])
    }

}
