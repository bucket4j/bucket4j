
package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.BucketExceptions
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.ExecutionException

import static io.github.bucket4j.util.PackageAccessor.getState
import static org.junit.jupiter.api.Assertions.assertNotSame
import static org.junit.jupiter.api.Assertions.fail;

class ConsumeIgnoringLimitsSpecification extends Specification {

    @Unroll
    def "#n case when limits are not overflown"(int n, long tokensToConsume, long nanosIncrement, long remainedTokens, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                for (boolean verbose : [false, true]) {
                    println("type=$type sync=$sync verbose=$verbose")
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    if (sync) {
                        Bucket bucket = type.createBucket(configuration, timeMeter)
                        bucket.getAvailableTokens()
                        timeMeter.addTime(nanosIncrement)
                        if (!verbose) {
                            assert bucket.consumeIgnoringRateLimits(tokensToConsume) == 0
                        } else {
                            def verboseResult = bucket.asVerbose().consumeIgnoringRateLimits(tokensToConsume)
                            assert verboseResult.value == 0L
                            if (type.isLocal()) {
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        }
                        assert bucket.getAvailableTokens() == remainedTokens
                    } else {
                        AsyncBucketProxy asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                        asyncBucket.getAvailableTokens().get()
                        timeMeter.addTime(nanosIncrement)
                        if (!verbose) {
                            asyncBucket.consumeIgnoringRateLimits(tokensToConsume).get() == 0
                        } else {
                            def verboseResult = asyncBucket.asVerbose().consumeIgnoringRateLimits(tokensToConsume).get()
                            verboseResult.value == 0L
                        }
                        assert asyncBucket.getAvailableTokens().get() == remainedTokens
                    }
                }
            }
        }
        where:
        n | tokensToConsume | nanosIncrement | remainedTokens | configuration
        1 |     49          |     50         |        1       | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()
        2 |     50          |     50         |        0       | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()
        3 |     51          |     120        |        49      | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()
        4 |     100         |     101        |        0       | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()
    }

    @Unroll
    def "#n case when limits are overflown"(int n, long tokensToConsume, long nanosIncrement, long remainedTokens, long overflowNanos, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                for (boolean verbose : [false, true]) {
                    println("type=$type sync=$sync verbose=$verbose")
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    if (sync) {
                        Bucket bucket = type.createBucket(configuration, timeMeter)
                        bucket.getAvailableTokens()
                        timeMeter.addTime(nanosIncrement)
                        if (!verbose) {
                            assert bucket.consumeIgnoringRateLimits(tokensToConsume) == overflowNanos
                        } else {
                            def verboseResult = bucket.asVerbose().consumeIgnoringRateLimits(tokensToConsume)
                            assert verboseResult.value == overflowNanos
                            if (type.isLocal()) {
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        }
                        assert bucket.getAvailableTokens() == remainedTokens
                    } else {
                        AsyncBucketProxy asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                        asyncBucket.getAvailableTokens().get()
                        timeMeter.addTime(nanosIncrement)
                        if (!verbose) {
                            assert asyncBucket.consumeIgnoringRateLimits(tokensToConsume).get() == overflowNanos
                        } else {
                            def verboseResult = asyncBucket.asVerbose().consumeIgnoringRateLimits(tokensToConsume).get()
                            assert verboseResult.value == overflowNanos
                        }
                        assert asyncBucket.getAvailableTokens().get() == remainedTokens
                    }
                }
            }
        }
        where:
        n | tokensToConsume | nanosIncrement | remainedTokens | overflowNanos   | configuration
        1 |     52          |      50        |       -2       |      2          | BucketConfiguration.builder().addLimit({it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)}).build()
        2 |     50          |      0         |      -50       |      50         | BucketConfiguration.builder().addLimit({it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)}).build()
        3 |    151          |      120       |      -51       |      51         | BucketConfiguration.builder().addLimit({it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)}).build()
        4 |    400          |      201       |      -300      |      300        | BucketConfiguration.builder().addLimit({it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)}).build()
    }

    def "Reservation overflow case"() {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(1).refillGreedy(1, Duration.ofMinutes(1)).initialTokens(0)})
                .build()
            long veryBigAmountOfTokensWhichCannotBeReserved = Long.MAX_VALUE / 2
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(configuration, timeMeter)
                if (sync) {
                    try {
                        bucket.consumeIgnoringRateLimits(veryBigAmountOfTokensWhichCannotBeReserved)
                        fail()
                    } catch (IllegalArgumentException e) {
                        assert e.message == BucketExceptions.reservationOverflow().message
                    }
                } else {
                    AsyncBucketProxy asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                    try {
                        asyncBucket.consumeIgnoringRateLimits(veryBigAmountOfTokensWhichCannotBeReserved).get()
                        fail()
                    } catch(ExecutionException e) {
                        assert e.getCause().message == BucketExceptions.reservationOverflow().message
                    }
                }
            }
        }
    }

    @Unroll
    // https://github.com/bucket4j/bucket4j/issues/417
    def "#type test consumption when amount of tokens became negative after consumeIgnoringRateLimits"(BucketType type) {
        expect:
            TimeMeterMock timeMeter = new TimeMeterMock(0)
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(5, Duration.ofSeconds(1))})
                .build()
            for (boolean sync : [true, false]) {
                for (boolean verbose : [false, true]) {
                    // println("type=$type sync=$sync verbose=$verbose")
                    if (sync) {
                        Bucket bucket = type.createBucket(configuration, timeMeter)
                        if (!verbose) {
                            bucket.consumeIgnoringRateLimits(15)
                            assert !bucket.tryConsume(1)
                            assert bucket.tryConsumeAsMuchAsPossible() == 0
                            assert bucket.tryConsumeAsMuchAsPossible(2) == 0
                            assert !bucket.estimateAbilityToConsume(2).canBeConsumed()
                            assert !bucket.tryConsumeAndReturnRemaining(2).isConsumed()
                        } else {
                            bucket.asVerbose().consumeIgnoringRateLimits(15)
                            assert !bucket.asVerbose().tryConsume(1).value
                            assert bucket.asVerbose().tryConsumeAsMuchAsPossible().value == 0
                            assert bucket.asVerbose().tryConsumeAsMuchAsPossible(2).value == 0
                            assert !bucket.asVerbose().estimateAbilityToConsume(2).value.canBeConsumed()
                            assert !bucket.asVerbose().tryConsumeAndReturnRemaining(2).value.isConsumed()
                        }
                    } else {
                        AsyncBucketProxy bucket = type.createAsyncBucket(configuration, timeMeter)
                        if (!verbose) {
                            bucket.consumeIgnoringRateLimits(15).get()
                            assert !bucket.tryConsume(1).get()
                            assert bucket.tryConsumeAsMuchAsPossible().get() == 0
                            assert bucket.tryConsumeAsMuchAsPossible(2).get() == 0
                            assert !bucket.estimateAbilityToConsume(2).get().canBeConsumed()
                            assert !bucket.tryConsumeAndReturnRemaining(2).get().isConsumed()
                        } else {
                            bucket.asVerbose().consumeIgnoringRateLimits(15).get()
                            assert !bucket.asVerbose().tryConsume(1).get().value
                            assert bucket.asVerbose().tryConsumeAsMuchAsPossible().get().value == 0
                            assert bucket.asVerbose().tryConsumeAsMuchAsPossible(2).get().value == 0
                            assert !bucket.asVerbose().estimateAbilityToConsume(2).get().value.canBeConsumed()
                            assert !bucket.asVerbose().tryConsumeAndReturnRemaining(2).get().value.isConsumed()
                        }
                    }
                }
            }
        where:
            type << BucketType.values()
    }

}
