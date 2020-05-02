
package io.github.bucket4j

import io.github.bucket4j.distributed.AsyncBucket
import io.github.bucket4j.mock.*
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS
import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION

class SchedulersSpecification extends Specification {

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep when trying to consuming #toConsume tokens from Bucket #configuration"(
            int n, long requiredSleep, long toConsume, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.withAsyncSupport()) {
            for (boolean limitAsDuration: [true, false]) {
                TimeMeterMock meter = new TimeMeterMock(0)
                AsyncBucket bucket = type.createAsyncBucket(configuration, meter)
                SchedulerMock scheduler = new SchedulerMock()

                if (limitAsDuration) {
                    bucket.asScheduler().tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), scheduler).get()
                } else {
                    bucket.asScheduler().tryConsume(toConsume, Duration.ofHours(1), scheduler).get()
                }
                assert scheduler.acummulatedDelayNanos == requiredSleep
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
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to synchronous consume #toConsume tokens with limit #sleepLimit from Bucket #configuration"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, BucketConfiguration configuration) {
        expect:
            for (BucketType type : BucketType.withAsyncSupport()) {
                for (boolean limitAsDuration: [true, false]) {
                    TimeMeterMock meter = new TimeMeterMock(0)
                    AsyncBucket bucket = type.createAsyncBucket(configuration, meter)
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
    def "should complete future exceptionally if scheduler failed to schedule the task"() {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(1, Duration.ofNanos(1)))
                    .build()
            GridBackendMock mockProxy = new GridBackendMock(SYSTEM_MILLISECONDS)
            SchedulerMock schedulerMock = new SchedulerMock()
            AsyncBucket bucket = mockProxy.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildAsyncProxy("66", configuration)
        when:
            schedulerMock.setException(new RuntimeException())
            CompletableFuture<Boolean> future = bucket.asScheduler().tryConsume(10, Duration.ofNanos(100000), schedulerMock)
        then:
            future.isCompletedExceptionally()
    }

    @Unroll
    def "#type test listener for blocking consume"(BucketType type) {
        setup:
            TimeMeterMock clock = new TimeMeterMock()
            BlockingStrategyMock blocker = new BlockingStrategyMock(clock)

            Bucket bucket = type.createBucket(Bucket4j.builder()
                    .withCustomTimePrecision(clock)
                    .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1))),
                clock)

        when:
            bucket.asScheduler().consume(9, blocker)
        then:
            blocker.parkedNanos == 0

        when:
            bucket.asScheduler().consume(2, blocker)
        then:
            blocker.parkedNanos == 100_000_000

        when:
            Thread.currentThread().interrupt()
            bucket.asScheduler().consume(1, blocker)
        then:
            thrown(InterruptedException)
            !Thread.interrupted()
            blocker.parkedNanos == 100_000_000
            blocker.atemptToParkNanos == 200_000_000

        when:
            bucket.asScheduler().consume(Long.MAX_VALUE, blocker)
        then:
            thrown(IllegalArgumentException)
            blocker.parkedNanos == 100_000_000

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for blocking consumeUninterruptibly"(BucketType type) {
        setup:
            TimeMeterMock clock = new TimeMeterMock()
            BlockingStrategyMock blocker = new BlockingStrategyMock(clock)

            Bucket bucket = type.createBucket(Bucket4j.builder()
                    .withCustomTimePrecision(clock)
                    .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1))),
                clock)

        when:
            bucket.asScheduler().consumeUninterruptibly(9, blocker)
        then:
            blocker.parkedNanos == 0

        when:
            bucket.asScheduler().consumeUninterruptibly(2, blocker)
        then:
            blocker.parkedNanos == 100_000_000

        when:
            Thread.currentThread().interrupt()
            bucket.asScheduler().consumeUninterruptibly(1, blocker)
        then:
            Thread.interrupted()
            blocker.parkedNanos == 200_000_000

        when:
            bucket.asScheduler().consumeUninterruptibly(Long.MAX_VALUE, blocker)
        then:
            thrown(IllegalArgumentException)
            blocker.parkedNanos == 200_000_000

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for async delayed consume"(BucketType type) {
        setup:
            TimeMeterMock clock = new TimeMeterMock()
            SchedulerMock scheduler = new SchedulerMock(clock)

            def configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
                    .build()
            AsyncBucket bucket = type.createAsyncBucket(configuration, clock)

        when:
            bucket.asScheduler().consume(9, scheduler).get()
        then:
            scheduler.acummulatedDelayNanos == 0

        when:
            bucket.asScheduler().consume(2, scheduler).get()
        then:
            scheduler.acummulatedDelayNanos == 100_000_000

        when:
            bucket.asScheduler().consume(Long.MAX_VALUE, scheduler).get()
        then:
            ExecutionException ex = thrown(ExecutionException)
            ex.getCause().class == IllegalArgumentException

        where:
            type << BucketType.withAsyncSupport()
    }

}
