package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.EstimationProbe
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

import static java.lang.Long.MAX_VALUE;

class EstimateAbilityToConsumeSpecification extends Specification {

    @Unroll
    def "#n estimateAbilityToConsume specification"(int n, long toEstimate, boolean result, long expectedWait, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0)
            Bucket bucket = type.createBucket(configuration, timeMeter)
            long availableTokensBeforeEstimation = bucket.getAvailableTokens()
            EstimationProbe probe = bucket.estimateAbilityToConsume(toEstimate)
            assert probe.canBeConsumed() == result
            assert probe.remainingTokens == availableTokensBeforeEstimation
            assert probe.nanosToWaitForRefill == expectedWait
            assert bucket.getAvailableTokens() == availableTokensBeforeEstimation

            if (type.asyncModeSupported) {
                AsyncBucketProxy asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                availableTokensBeforeEstimation = bucket.getAvailableTokens()
                probe = asyncBucket.estimateAbilityToConsume(toEstimate).get()
                assert probe.canBeConsumed() == result
                assert probe.remainingTokens == availableTokensBeforeEstimation
                assert probe.nanosToWaitForRefill == expectedWait
                assert bucket.getAvailableTokens() == availableTokensBeforeEstimation
            }
        }
        where:
        n | toEstimate | result  |  expectedWait | configuration
        1 |    49      |   true  |        0      | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(100)).build()
        2 |     1      |   true  |        0      | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(1)).build()
        3 |    80      |   false |       10      | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(70)).build()
        4 |    10      |   false |       10      | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()
        5 |   120      |   false |    MAX_VALUE  | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(10)).build()
        6 |    80      |   false |      100      | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillIntervally(100, Duration.ofNanos(100)).initialTokens(70)).build()
        7 |    10      |   false |      100      | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillIntervally(100, Duration.ofNanos(100)).initialTokens(0)).build()
        8 |   120      |   false |   MAX_VALUE   | BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillIntervally(100, Duration.ofNanos(100)).initialTokens(10)).build()
    }

}
