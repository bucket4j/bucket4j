
package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.BucketExceptions
import io.github.bucket4j.distributed.AsyncBucket;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import spock.lang.Specification;
import spock.lang.Unroll;

import java.time.Duration
import java.util.concurrent.ExecutionException

import static io.github.bucket4j.util.PackageAccessor.getState
import static org.junit.Assert.assertNotSame
import static org.junit.Assert.fail;

class ConsumeIgnoringLimitsSpecification extends Specification {

    @Unroll
    def "#n case when limits are not overflown"(int n, long tokensToConsume, long nanosIncrement, long remainedTokens, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                for (boolean verbose : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    if (sync) {
                        Bucket bucket = type.createBucket(configuration, timeMeter)
                        timeMeter.addTime(nanosIncrement)
                        if (!verbose) {
                            assert bucket.consumeIgnoringRateLimits(tokensToConsume) == 0
                        } else {
                            def verboseResult = bucket.asVerbose().consumeIgnoringRateLimits(tokensToConsume)
                            assert verboseResult.value == 0
                            assertNotSame(verboseResult.state, getState(bucket))
                        }
                        assert bucket.createSnapshot().getAvailableTokens(bucket.configuration.bandwidths) == remainedTokens
                    } else if (type.asyncModeSupported) {
                        AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                        if (!verbose) {
                            asyncBucket.consumeIgnoringRateLimits(tokensToConsume).get() == 0
                        } else {
                            def verboseResult = asyncBucket.asVerbose().consumeIgnoringRateLimits(tokensToConsume).get()
                            verboseResult.value == 0
                            assertNotSame(verboseResult.state, getState(asyncBucket))
                        }
                    }
                }
            }
        }
        where:
        n | tokensToConsume | nanosIncrement | remainedTokens | configuration
        1 |     49          |     50         |        1       | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        2 |     50          |     50         |        0       | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        3 |     51          |     120        |        49      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        4 |     100         |     101        |        0       | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
    }

    @Unroll
    def "#n case when limits are overflown"(int n, long tokensToConsume, long nanosIncrement, long remainedTokens, long overflowNanos, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                for (boolean verbose : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    if (sync) {
                        Bucket bucket = type.createBucket(configuration, timeMeter)
                        timeMeter.addTime(nanosIncrement)
                        assert bucket.createSnapshot().getAvailableTokens(bucket.configuration.bandwidths) == remainedTokens
                        if (!verbose) {
                            assert bucket.consumeIgnoringRateLimits(tokensToConsume) == overflowNanos
                        } else {
                            def verboseResult = bucket.asVerbose().consumeIgnoringRateLimits(tokensToConsume)
                            assert verboseResult.value == overflowNanos
                            assertNotSame(verboseResult.state, getState(bucket))
                        }
                    } else if (type.isAsyncModeSupported()) {
                        AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                        if (!verbose) {
                            if (type.isAsyncModeSupported()) {
                                assert asyncBucket.consumeIgnoringRateLimits(tokensToConsume).get() == overflowNanos
                            }
                        } else {
                            def verboseResult = asyncBucket.asVerbose().consumeIgnoringRateLimits(tokensToConsume).get()
                            assert verboseResult.value == overflowNanos
                            assertNotSame(verboseResult.state, getState(asyncBucket))
                        }
                    }
                }
            }
        }
        where:
        n | tokensToConsume | nanosIncrement | remainedTokens | overflowNanos   | configuration
        1 |     52          |      50        |       -2       |      2          | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        2 |     50          |      0         |      -50       |      50         | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        3 |    151          |      120       |      -51       |      51         | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        4 |    400          |      201       |      -300      |      300        | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
    }

    def "Reservation overflow case"() {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(1, Duration.ofMinutes(1)).withInitialTokens(0))
            long veryBigAmountOfTokensWhichCannotBeReserved = Long.MAX_VALUE / 2;
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
                } else if (type.asyncModeSupported) {
                    AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
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

}
