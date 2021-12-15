package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class ForceAddTokensSpecification extends Specification {

    @Unroll
    def "#n Force Add tokens spec"(
            int n, long tokensToAdd, long nanosIncrement, long requiredResult, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                for (boolean verbose : [true, false]) {
                    println("type=$type sync=$sync verbose=$verbose")
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    if (sync) {
                        Bucket bucket = type.createBucket(configuration, timeMeter)
                        bucket.getAvailableTokens()
                        timeMeter.addTime(nanosIncrement)
                        if (!verbose) {
                            bucket.forceAddTokens(tokensToAdd)
                        } else {
                            bucket.asVerbose().forceAddTokens(tokensToAdd)
                        }
                        assert bucket.getAvailableTokens() == requiredResult
                    } else {
                        AsyncBucketProxy bucket = type.createAsyncBucket(configuration, timeMeter)
                        bucket.getAvailableTokens()
                        timeMeter.addTime(nanosIncrement)
                        if (!verbose) {
                            bucket.forceAddTokens(tokensToAdd).get()
                        } else {
                            bucket.asVerbose().forceAddTokens(tokensToAdd).get()
                        }
                        assert bucket.getAvailableTokens().get() == requiredResult
                    }
                }
            }
        }
        where:
        n | tokensToAdd | nanosIncrement | requiredResult | configuration
        1 |     49      |     50         |        99      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        2 |     50      |     50         |       100      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        3 |     50      |     0          |        50      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        4 |     120     |     0          |       120      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        5 |     120     |     110        |       220      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
    }

    def "Tokens that was added over capacity should not be lost"() {
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
