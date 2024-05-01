package io.github.bucket4j.api_specifications.blocking

import io.github.bucket4j.BlockingStrategy
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.SimpleBucketListener
import io.github.bucket4j.TimeMeter
import io.github.bucket4j.mock.BlockingStrategyMock
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import io.github.bucket4j.util.PipeGenerator;
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.TimeUnit

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS

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

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep when trying to consuming #toConsume tokens from Bucket #configuration"(
            int n, long requiredSleep, long toConsume, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean uniterruptible : [true, false]) {
                for (boolean limitAsDuration: [true, false]) {
                    for (boolean verbose: [true, false]) {
                        TimeMeterMock meter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(configuration, meter)
                        BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                        if (uniterruptible) {
                            if (limitAsDuration) {
                                if (verbose) {
                                    bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(toConsume, Duration.ofHours(1), sleepStrategy)
                                } else {
                                    bucket.asBlocking().tryConsumeUninterruptibly(toConsume, Duration.ofHours(1), sleepStrategy)
                                }
                            } else {
                                if (verbose) {
                                    bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                                } else {
                                    bucket.asBlocking().tryConsumeUninterruptibly(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                                }
                            }
                        } else {
                            if (limitAsDuration) {
                                if (verbose) {
                                    bucket.asBlocking().asVerbose().tryConsume(toConsume, Duration.ofHours(1), sleepStrategy)
                                } else {
                                    bucket.asBlocking().tryConsume(toConsume, Duration.ofHours(1), sleepStrategy)
                                }
                            } else {
                                if (verbose) {
                                    bucket.asBlocking().asVerbose().tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                                } else {
                                    bucket.asBlocking().tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                                }
                            }
                        }
                        assert sleepStrategy.parkedNanos == requiredSleep
                    }
                }
            }
        }
        where:
        n | requiredSleep | toConsume | configuration
        1 |      10       |     1     | BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0)}).build()
        2 |       0       |     1     | BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)}).build()
        3 |    9990       |  1000     | BucketConfiguration.builder().addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1)}).build()
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to synchronous consume #toConsume tokens with limit #sleepLimit"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean uniterruptible : [true, false]) {
                for (boolean limitAsDuration: [true, false]) {
                    for (boolean verbose: [true, false]) {
                        TimeMeterMock meter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(configuration, meter)
                        BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                        if (uniterruptible) {
                            if (limitAsDuration) {
                                if (verbose) {
                                    assert bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy).value == requiredResult
                                } else {
                                    assert bucket.asBlocking().tryConsumeUninterruptibly(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy) == requiredResult
                                }
                            } else {
                                if (verbose) {
                                    assert bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(toConsume, sleepLimit, sleepStrategy).value == requiredResult
                                } else {
                                    assert bucket.asBlocking().tryConsumeUninterruptibly(toConsume, sleepLimit, sleepStrategy) == requiredResult
                                }
                            }
                        } else {
                            if (limitAsDuration) {
                                if (verbose) {
                                    assert bucket.asBlocking().asVerbose().tryConsume(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy).value == requiredResult
                                } else {
                                    assert bucket.asBlocking().tryConsume(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy) == requiredResult
                                }
                            } else {
                                if (verbose) {
                                    assert bucket.asBlocking().asVerbose().tryConsume(toConsume, sleepLimit, sleepStrategy).value == requiredResult
                                } else {
                                    assert bucket.asBlocking().tryConsume(toConsume, sleepLimit, sleepStrategy) == requiredResult
                                }
                            }
                        }
                        assert sleepStrategy.parkedNanos == requiredSleep
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

    @Unroll
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    def "#type Should throw InterruptedException when thread interrupted during waiting for token refill #verbose #meter"(BucketType type, boolean verbose, TimeMeter meter) {
        expect:
            BucketConfiguration config = BucketConfiguration.builder()
                .addLimit({ limit -> limit.capacity(1).refillGreedy(1, Duration.ofMinutes(1)).initialTokens(0)})
                .build()
            Bucket bucket = type.createBucket(config, meter)

            Thread.currentThread().interrupt()
            InterruptedException thrown1
            try {
                bucket.asBlocking().tryConsume(1, TimeUnit.HOURS.toNanos(1000), BlockingStrategy.PARKING)
            } catch (InterruptedException e) {
                thrown1 = e
            }
            assert thrown1 != null

            InterruptedException thrown2
            Thread.currentThread().interrupt()
            try {
                bucket.asBlocking().tryConsume(1, TimeUnit.HOURS.toNanos(1), BlockingStrategy.PARKING)
            } catch (InterruptedException e) {
                thrown2 = e
            }
            assert thrown2 != null

        where:
            [type, verbose, meter] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true], [SYSTEM_MILLISECONDS, TimeMeter.SYSTEM_NANOTIME])
    }

    @Unroll
    def "#type test listener for blocking tryConsume #verbose"(BucketType type, boolean verbose) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            Bucket bucket = type.createBucket(configuration, clock, listener)

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().tryConsume(9, Duration.ofSeconds(1), blocker)
            } else {
                bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().tryConsume(1000, Duration.ofSeconds(1), blocker)
            } else {
                bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().tryConsume(2, Duration.ofSeconds(1), blocker)
            } else {
                bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker)
            }
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.beforeParkingNanos == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            if (verbose) {
                bucket.asBlocking().asVerbose().tryConsume(1, Duration.ofSeconds(1), blocker)
            } else {
                bucket.asBlocking().tryConsume(1, Duration.ofSeconds(1), blocker)
            }
        then:
            thrown(InterruptedException)
            listener.getConsumed() == 12
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.beforeParkingNanos == 200_000_000
            listener.getInterrupted() == 1

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    @Unroll
    def "#type test listener for blocking tryConsumeUninterruptibly #verbose"(BucketType type, boolean verbose) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            Bucket bucket = type.createBucket(configuration, clock, listener)

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().tryConsume(9, Duration.ofSeconds(1), blocker)
            } else {
                bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().tryConsume(1000, Duration.ofSeconds(1), blocker)
            } else {
                bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().tryConsume(2, Duration.ofSeconds(1), blocker)
            } else {
                bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker)
            }
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.beforeParkingNanos == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            if (verbose) {
                bucket.asBlocking().asVerbose().tryConsumeUninterruptibly(1, Duration.ofSeconds(1), blocker)
            } else {
                bucket.asBlocking().tryConsumeUninterruptibly(1, Duration.ofSeconds(1), blocker)
            }
            Thread.interrupted()
        then:
            listener.getConsumed() == 12
            listener.getRejected() == 1000
            listener.getParkedNanos() == 200_000_000
            listener.beforeParkingNanos == 200_000_000
            listener.getInterrupted() == 0

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

}
