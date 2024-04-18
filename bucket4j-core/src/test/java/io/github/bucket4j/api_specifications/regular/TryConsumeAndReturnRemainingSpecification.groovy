package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.SimpleBucketListener
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import io.github.bucket4j.util.PipeGenerator
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

import static java.lang.Long.MAX_VALUE

class TryConsumeAndReturnRemainingSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    SimpleBucketListener listener = new SimpleBucketListener()

    BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build()

    @Unroll
    def "#n tryConsumeAndReturnRemaining specification"(int n, long toConsume, boolean result, long expectedRemaining, long expectedWait, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0)
            Bucket bucket = type.createBucket(configuration, timeMeter)
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(toConsume)
            assert probe.consumed == result
            assert probe.remainingTokens == expectedRemaining
            assert probe.nanosToWaitForRefill == expectedWait

            AsyncBucketProxy asyncBucket = type.createAsyncBucket(configuration, timeMeter)
            probe = asyncBucket.tryConsumeAndReturnRemaining(toConsume).get()
            assert probe.consumed == result
            assert probe.remainingTokens == expectedRemaining
            assert probe.nanosToWaitForRefill == expectedWait

        }
        where:
        n | toConsume | result  |  expectedRemaining | expectedWait | configuration
        1 |    49     |   true  |           51       |       0      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(100)).build()
        2 |     1     |   true  |            0       |       0      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1)).build()
        3 |    80     |   false |           70       |      10      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70)).build()
        4 |    10     |   false |            0       |      10      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        5 |   120     |   false |           10       |   MAX_VALUE  | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(10)).build()
    }

    @Unroll
    def "#type verbose=#verbose test listener for tryConsumeAndReturnRemaining"(BucketType type, boolean verbose) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock, listener)

        when:
            if (!verbose) {
                bucket.tryConsumeAndReturnRemaining(9)
            } else {
                bucket.asVerbose().tryConsumeAndReturnRemaining(9)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAndReturnRemaining(6)
            } else {
                bucket.asVerbose().tryConsumeAndReturnRemaining(6)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    @Unroll
    def "#type verbose=#verbose test listener for async tryConsumeAndReturnRemaining"(BucketType type, boolean verbose) {
        setup:
            AsyncBucketProxy bucket = type.createAsyncBucket(configuration, clock, listener)

        when:
            if (!verbose) {
                bucket.tryConsumeAndReturnRemaining(9).get()
            } else {
                bucket.asVerbose().tryConsumeAndReturnRemaining(9).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAndReturnRemaining(6).get()
            } else {
                bucket.asVerbose().tryConsumeAndReturnRemaining(6).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

}
