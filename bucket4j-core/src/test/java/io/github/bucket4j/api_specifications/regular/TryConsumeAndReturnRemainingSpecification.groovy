package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.distributed.AsyncBucket
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class TryConsumeAndReturnRemainingSpecification extends Specification {

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
            if (type.isAsyncModeSupported()) {
                AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                probe = asyncBucket.tryConsumeAndReturnRemaining(toConsume).get()
                assert probe.consumed == result
                assert probe.remainingTokens == expectedRemaining
                assert probe.nanosToWaitForRefill == expectedWait
            }
        }
        where:
        n | toConsume | result  |  expectedRemaining | expectedWait | configuration
        1 |    49     |   true  |           51       |       0      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(100)).build()
        2 |     1     |   true  |            0       |       0      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1)).build()
        3 |    80     |   false |           70       |      10      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70)).build()
        4 |    10     |   false |            0       |      10      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        5 |   120     |   false |           10       |     110      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(10)).build()
    }

}
