
package io.github.bucket4j

import io.github.bucket4j.grid.GridBucket
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.GridProxyMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import static io.github.bucket4j.PackageAcessor.getState
import static org.junit.Assert.assertNotSame

import java.time.Duration
import java.util.concurrent.CompletableFuture

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS
import static io.github.bucket4j.grid.RecoveryStrategy.*

class BucketSpecification extends Specification {

    @Unroll
    def "#n Should return #requiredResult when trying to consume single token from Bucket #builder"(
            int n, boolean requiredResult, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean verbose : [true, false]) {
                        def timeMeter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(builder, timeMeter)
                        if (sync) {
                            if (!verbose) {
                                assert bucket.tryConsume(1) == requiredResult
                            } else {
                                def verboseResult = bucket.asVerbose().tryConsume(1)
                                assert verboseResult.value == requiredResult
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        } else {
                            if (!verbose) {
                                assert bucket.asAsync().tryConsume(1).get() == requiredResult
                            } else {
                                def verboseResult = bucket.asAsync().asVerbose().tryConsume(1).get()
                                assert verboseResult.value == requiredResult
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        }
                    }
                }
            }
        where:
            n | requiredResult |  builder
            1 |     false      |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0))
            2 |      true      |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(1))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consume #toConsume tokens from Bucket #builder"(
            int n, boolean requiredResult, long toConsume, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean verbose : [true, false]) {
                        def timeMeter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(builder, timeMeter)
                        if (sync) {
                            if (!verbose) {
                                assert bucket.tryConsume(toConsume) == requiredResult
                            } else {
                                def verboseResult = bucket.asVerbose().tryConsume(toConsume)
                                assert verboseResult.value == requiredResult
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        } else {
                            if (!verbose) {
                                assert bucket.asAsync().tryConsume(toConsume).get() == requiredResult
                            } else {
                                def verboseResult = bucket.asAsync().asVerbose().tryConsume(toConsume).get()
                                assert verboseResult.value == requiredResult
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        }
                    }
                }
            }
        where:
            n | requiredResult | toConsume | builder
            1 |     false      |     1     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0))
            2 |      true      |     1     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(1))
    }

    @Unroll
    def "#n tryConsumeAndReturnRemaining specification"(int n, long toConsume, boolean result, long expectedRemaining, long expectedWait, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean verbose : [true, false]) {
                        TimeMeterMock timeMeter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(builder, timeMeter)
                        ConsumptionProbe probe
                        if (sync) {
                            if (!verbose) {
                                probe = bucket.tryConsumeAndReturnRemaining(toConsume)
                            } else {
                                def verboseResult = bucket.asVerbose().tryConsumeAndReturnRemaining(toConsume)
                                probe = verboseResult.value
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        } else {
                            if (!verbose) {
                                probe = bucket.asAsync().tryConsumeAndReturnRemaining(toConsume).get()
                            } else {
                                def verboseResult = bucket.asAsync().asVerbose().tryConsumeAndReturnRemaining(toConsume).get()
                                probe = verboseResult.value
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        }
                        assert probe.consumed == result
                        assert probe.remainingTokens == expectedRemaining
                        assert probe.nanosToWaitForRefill == expectedWait
                    }
                }
            }
        where:
            n | toConsume | result  |  expectedRemaining | expectedWait | builder
            1 |    49     |   true  |           51       |       0      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(100))
            2 |     1     |   true  |            0       |       0      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
            3 |    80     |   false |           70       |      10      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70))
            4 |    10     |   false |            0       |      10      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            5 |   120     |   false |           10       |     110      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(10))
    }

    @Unroll
    def "#n estimateAbilityToConsume specification"(int n, long toEstimate, boolean result, long expectedWait, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean verbose : [true, false]) {
                        TimeMeterMock timeMeter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(builder, timeMeter)
                        long availableTokensBeforeEstimation = bucket.getAvailableTokens()

                        EstimationProbe probe
                        if (sync) {
                            if (!verbose) {
                                probe = bucket.estimateAbilityToConsume(toEstimate)
                            } else {
                                def verboseResult = bucket.asVerbose().estimateAbilityToConsume(toEstimate)
                                probe = verboseResult.value
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        } else {
                            if (!verbose) {
                                probe = bucket.asAsync().estimateAbilityToConsume(toEstimate).get()
                            } else {
                                def verboseResult = bucket.asAsync().asVerbose().estimateAbilityToConsume(toEstimate).get()
                                probe = verboseResult.value
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        }
                        assert probe.canBeConsumed() == result
                        assert probe.remainingTokens == availableTokensBeforeEstimation
                        assert probe.nanosToWaitForRefill == expectedWait
                        assert bucket.getAvailableTokens() == availableTokensBeforeEstimation
                    }
                }
            }
        where:
            n | toEstimate | result  |  expectedWait | builder
            1 |    49      |   true  |        0      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(100))
            2 |     1      |   true  |        0      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
            3 |    80      |   false |       10      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70))
            4 |    10      |   false |       10      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            5 |   120      |   false |      110      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(10))
            6 |    80      |   false |      100      | Bucket4j.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofNanos(100))).withInitialTokens(70))
            7 |    10      |   false |      100      | Bucket4j.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofNanos(100))).withInitialTokens(0))
            8 |   120      |   false |      200      | Bucket4j.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofNanos(100))).withInitialTokens(10))
    }

    @Unroll
    def "#n Should return #requiredResult when consumeAsMuchAsPossible tokens from Bucket #builder"(
            int n, long requiredResult, AbstractBucketBuilder builder) {
        expect:
            for (BucketType bucketType : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean verbose : [true, false]) {
                        TimeMeterMock timeMeter = new TimeMeterMock(0)
                        Bucket bucket = bucketType.createBucket(builder, timeMeter)
                        if (sync) {
                            if (!verbose) {
                                assert bucket.tryConsumeAsMuchAsPossible() == requiredResult
                            } else {
                                def verboseResult = bucket.asVerbose().tryConsumeAsMuchAsPossible()
                                assert verboseResult.value == requiredResult
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        } else {
                            if (!verbose) {
                                assert bucket.asAsync().tryConsumeAsMuchAsPossible().get() == requiredResult
                            } else {
                                def verboseResult = bucket.asAsync().asVerbose().tryConsumeAsMuchAsPossible().get()
                                assert verboseResult.value == requiredResult
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        }
                    }
                }
            }
        where:
            n | requiredResult | builder
            1 |        0       | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0))
            2 |        2       | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(2))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consumeAsMuchAsPossible with limit #limit tokens from Bucket #builder"(
            int n, long requiredResult, long limit, AbstractBucketBuilder builder) {
        expect:
            for (BucketType bucketType : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean verbose : [true, false]) {
                        TimeMeterMock timeMeter = new TimeMeterMock(0)
                        Bucket bucket = bucketType.createBucket(builder, timeMeter)
                        if (sync) {
                            if (!verbose) {
                                assert bucket.tryConsumeAsMuchAsPossible(limit) == requiredResult
                            } else {
                                def verboseResult = bucket.asVerbose().tryConsumeAsMuchAsPossible(limit)
                                assert verboseResult.value == requiredResult
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        } else {
                            if (!verbose) {
                                assert bucket.asAsync().tryConsumeAsMuchAsPossible(limit).get() == requiredResult
                            } else {
                                def verboseResult = bucket.asAsync().asVerbose().tryConsumeAsMuchAsPossible(limit).get()
                                assert verboseResult.value == requiredResult
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        }
                    }
                }
            }
        where:
            n | requiredResult |   limit   | builder
            1 |       4        |     5     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(4))
            2 |       5        |     5     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5))
            3 |       5        |     5     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5))
    }

    @Unroll
    def "#n Add tokens spec"(
            int n, long tokensToAdd, long nanosIncrement, long requiredResult, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean verbose : [true, false]) {
                        TimeMeterMock timeMeter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(builder, timeMeter)
                        timeMeter.addTime(nanosIncrement)
                        if (sync) {
                            if (!verbose) {
                                bucket.addTokens(tokensToAdd)
                            } else {
                                def verboseResult = bucket.asVerbose().addTokens(tokensToAdd)
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        } else {
                            if (!verbose) {
                                bucket.asAsync().addTokens(tokensToAdd).get()
                            } else {
                                def verboseResult = bucket.asAsync().asVerbose().addTokens(tokensToAdd).get()
                                assertNotSame(verboseResult.state, getState(bucket))
                            }
                        }
                        assert bucket.createSnapshot().getAvailableTokens(bucket.configuration.bandwidths) == requiredResult
                    }
                }
            }
        where:
            n | tokensToAdd | nanosIncrement | requiredResult | builder
            1 |     49      |     50         |        99      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            2 |     50      |     50         |       100      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            3 |     50      |     0          |        50      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            4 |     120     |     0          |       100      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            5 |     120     |     110        |       100      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
    }

    @Unroll
    def "#n getAvailableTokens specification"(int n, long nanosSinceBucketCreation, long expectedTokens,  AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean verbose : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    timeMeter.addTime(nanosSinceBucketCreation)
                    if (!verbose) {
                        assert bucket.getAvailableTokens() == expectedTokens
                    } else {
                        assert bucket.asVerbose().getAvailableTokens().value == expectedTokens
                    }
                }
            }
        where:
            n | nanosSinceBucketCreation | expectedTokens |  builder
            1 |             49           |     50         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
            2 |             50           |     50         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            3 |             50           |    100         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70))
            4 |              0           |      0         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            5 |            120           |    100         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
    }

    def "should complete future exceptionally if backend failed"() {
        setup:
            BucketConfiguration configuration = Bucket4j.configurationBuilder()
                                                    .addLimit(Bandwidth.simple(1, Duration.ofNanos(1)))
                                                    .build()
            GridProxyMock mockProxy = new GridProxyMock(SYSTEM_MILLISECONDS);
            Bucket bucket = GridBucket.createInitializedBucket("66", configuration, mockProxy, THROW_BUCKET_NOT_FOUND_EXCEPTION)
        when:
            mockProxy.setException(new RuntimeException())
            CompletableFuture<Boolean> future = bucket.asAsync().tryConsume(1)
        then:
            future.isCompletedExceptionally()
    }

    def "check that toString does not fail"() {
        when:
            for (BucketType type : BucketType.values()) {
                for (TimeMeter meter : [SYSTEM_MILLISECONDS, SYSTEM_MILLISECONDS]) {
                    AbstractBucketBuilder builder = Bucket4j.builder()
                            .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
                    Bucket bucket = type.createBucket(builder, meter)
                    println bucket.toString()
                }
            }
        then:
            noExceptionThrown()
    }

}
