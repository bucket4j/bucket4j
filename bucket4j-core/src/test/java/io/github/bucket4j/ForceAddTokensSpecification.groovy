package io.github.bucket4j

import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

import static io.github.bucket4j.PackageAcessor.getState
import static io.github.bucket4j.PackageAcessor.getState
import static org.junit.Assert.assertNotSame
import static org.junit.Assert.assertNotSame

class ForceAddTokensSpecification extends Specification {

    @Unroll
    def "#n Force Add tokens spec"(
            int n, long tokensToAdd, long nanosIncrement, long requiredResult, AbstractBucketBuilder builder) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                for (boolean verbose : [true, false]) {
                    // println("type=$type sync=$sync verbose=$verbose")
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    timeMeter.addTime(nanosIncrement)
                    if (sync) {
                        if (!verbose) {
                            bucket.forceAddTokens(tokensToAdd)
                        } else {
                            def verboseResult = bucket.asVerbose().forceAddTokens(tokensToAdd)
                            assertNotSame(verboseResult.state, getState(bucket))
                        }
                    } else {
                        if (!verbose) {
                            bucket.asAsync().forceAddTokens(tokensToAdd).get()
                        } else {
                            def verboseResult = bucket.asAsync().asVerbose().forceAddTokens(tokensToAdd).get()
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
        4 |     120     |     0          |       120      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
        5 |     120     |     110        |       220      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
    }

    @Unroll
    def "#n Tokens that was added over capacity should not be lost"() {
        setup:
            TimeMeterMock timeMeter = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                    .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)))
                    .withCustomTimePrecision(timeMeter)
                    .build()
        when:
            bucket.forceAddTokens(10)
        then:
            bucket.getAvailableTokens() == 110

        when:
            timeMeter.addTime(10)
            bucket.consumeIgnoringRateLimits(2)
        then:
            bucket.getAvailableTokens() == 108

        when:
            timeMeter.addTime(10)
            bucket.tryConsume(3)
        then:
            bucket.getAvailableTokens() == 105
    }

}
