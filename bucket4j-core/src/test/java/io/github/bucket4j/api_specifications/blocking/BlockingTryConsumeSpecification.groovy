package io.github.bucket4j.api_specifications.blocking

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BlockingStrategy
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.SimpleBucketListener
import io.github.bucket4j.TimeMeter
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.BlockingStrategyMock
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.ProxyManagerMock
import io.github.bucket4j.mock.SchedulerMock
import io.github.bucket4j.mock.TimeMeterMock;
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS
import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;

class BlockingTryConsumeSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    BlockingStrategyMock blocker = new BlockingStrategyMock(clock)
    SimpleBucketListener listener = new SimpleBucketListener() {
        long beforeParkingNanos;
        @Override
        void beforeParking(long nanos) {
            beforeParkingNanos += nanos
        }
    }
    SchedulerMock scheduler = new SchedulerMock(clock)

    BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
            .build()

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep when trying to consuming #toConsume tokens from Bucket #configuration"(
            int n, long requiredSleep, long toConsume, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean uniterruptible : [true, false]) {
                for (boolean limitAsDuration: [true, false]) {
                    TimeMeterMock meter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(configuration, meter)
                    BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                    if (uniterruptible) {
                        if (limitAsDuration) {
                            bucket.asBlocking().tryConsumeUninterruptibly(toConsume, Duration.ofHours(1), sleepStrategy)
                        } else {
                            bucket.asBlocking().tryConsumeUninterruptibly(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                        }
                    } else {
                        if (limitAsDuration) {
                            bucket.asBlocking().tryConsume(toConsume, Duration.ofHours(1), sleepStrategy)
                        } else {
                            bucket.asBlocking().tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                        }
                    }
                    assert sleepStrategy.parkedNanos == requiredSleep
                }
            }
        }
        where:
        n | requiredSleep | toConsume | configuration
        1 |      10       |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0)).build()
        2 |       0       |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        3 |    9990       |  1000     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to synchronous consume #toConsume tokens with limit #sleepLimit"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean uniterruptible : [true, false]) {
                for (boolean limitAsDuration: [true, false]) {
                    TimeMeterMock meter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(configuration, meter)
                    BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                    if (uniterruptible) {
                        if (limitAsDuration) {
                            assert bucket.asBlocking().tryConsumeUninterruptibly(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy) == requiredResult
                        } else {
                            assert bucket.asBlocking().tryConsumeUninterruptibly(toConsume, sleepLimit, sleepStrategy) == requiredResult
                        }
                    } else {
                        if (limitAsDuration) {
                            assert bucket.asBlocking().tryConsume(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy) == requiredResult
                        } else {
                            assert bucket.asBlocking().tryConsume(toConsume, sleepLimit, sleepStrategy) == requiredResult
                        }
                    }
                    assert sleepStrategy.parkedNanos == requiredSleep
                }
            }
        }
        where:
        n | requiredSleep | requiredResult | toConsume | sleepLimit |  configuration
        1 |      10       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0)).build()
        2 |      10       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0)).build()
        3 |       0       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        4 |       0       |     false      |   1000    |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        5 |      40       |     true       |     5     |     40     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        6 |      40       |     true       |     5     |     41     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        6 |       0       |     false      |     5     |     39     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    def "Should throw InterruptedException when thread interrupted during waiting for token refill"() {
        expect:
        for (TimeMeter meter : [SYSTEM_MILLISECONDS, TimeMeter.SYSTEM_NANOTIME]) {
            Bucket bucket = Bucket.builder()
                    .withCustomTimePrecision(meter)
                    .addLimit(Bandwidth.simple(1, Duration.ofMinutes(1)).withInitialTokens(0))
                    .build()

            Thread.currentThread().interrupt()
            InterruptedException thrown
            try {
                bucket.asBlocking().tryConsume(1, TimeUnit.HOURS.toNanos(1000), BlockingStrategy.PARKING)
            } catch (InterruptedException e) {
                thrown = e
            }
            assert thrown != null

            thrown = null
            Thread.currentThread().interrupt()
            try {
                bucket.asBlocking().tryConsume(1, TimeUnit.HOURS.toNanos(1), BlockingStrategy.PARKING)
            } catch (InterruptedException e) {
                thrown = e
            }
            assert thrown != null
        }
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to synchronous consume #toConsume tokens with limit #sleepLimit from Bucket #configuration"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean limitAsDuration: [true, false]) {
                TimeMeterMock meter = new TimeMeterMock(0)
                AsyncBucketProxy bucket = type.createAsyncBucket(configuration, meter)
                SchedulerMock scheduler = new SchedulerMock()
                if (limitAsDuration) {
                    assert bucket.asScheduler().tryConsume(toConsume, Duration.ofNanos(sleepLimit), scheduler).get() == requiredResult
                } else {
                    assert bucket.asScheduler().tryConsume(toConsume, sleepLimit, scheduler).get() == requiredResult
                }
                assert scheduler.acummulatedDelayNanos == requiredSleep
            }
        }
        where:
        n | requiredSleep | requiredResult | toConsume | sleepLimit |  configuration
        1 |      10       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0)).build()
        2 |      10       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0)).build()
        3 |       0       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        4 |       0       |     false      |   1000    |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        5 |      40       |     true       |     5     |     40     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        6 |      40       |     true       |     5     |     41     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        6 |       0       |     false      |     5     |     39     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    def "should complete future exceptionally if scheduler failed to schedule the task"() {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1, Duration.ofNanos(1)))
                .build()
            ProxyManagerMock mockProxy = new ProxyManagerMock(SYSTEM_MILLISECONDS)
            SchedulerMock schedulerMock = new SchedulerMock()
            AsyncBucketProxy bucket = mockProxy.asAsync().builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .build("66", configuration)
        when:
            schedulerMock.setException(new RuntimeException())
            CompletableFuture<Boolean> future = bucket.asScheduler().tryConsume(10, Duration.ofNanos(100000), schedulerMock)
        then:
            future.isCompletedExceptionally()
    }

    @Unroll
    def "#type test listener for blocking tryConsume"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.beforeParkingNanos == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asBlocking().tryConsume(1, Duration.ofSeconds(1), blocker)
        then:
            thrown(InterruptedException)
            listener.getConsumed() == 12
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.beforeParkingNanos == 200_000_000
            listener.getInterrupted() == 1

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for blocking tryConsumeUninterruptibly"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.beforeParkingNanos == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asBlocking().tryConsumeUninterruptibly(1, Duration.ofSeconds(1), blocker)
            Thread.interrupted()
        then:
            listener.getConsumed() == 12
            listener.getRejected() == 1000
            listener.getParkedNanos() == 200_000_000
            listener.beforeParkingNanos == 200_000_000
            listener.getInterrupted() == 0

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for async scheduled tryConsume"(BucketType type) {
        setup:
            AsyncBucketProxy bucket = type.createAsyncBucket(configuration, clock).toListenable(listener)

        when:
            bucket.asScheduler().tryConsume(9, Duration.ofSeconds(1).toNanos(), scheduler)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().tryConsume(1000, Duration.ofSeconds(1).toNanos(), scheduler)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().tryConsume(2, Duration.ofSeconds(1).toNanos(), scheduler)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getDelayedNanos() == 100_000_000
            listener.getInterrupted() == 0

        where:
            type << BucketType.values()
    }


}
