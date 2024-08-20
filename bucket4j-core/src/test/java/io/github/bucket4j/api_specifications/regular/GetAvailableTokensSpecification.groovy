package io.github.bucket4j.api_specifications.regular


import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class GetAvailableTokensSpecification extends Specification {

    @Unroll
    def "#n getAvailableTokens specification"(int n, long nanosSinceBucketCreation, long expectedTokens, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean verbose : [true, false]) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(configuration, timeMeter)
                bucket.estimateAbilityToConsume(1) // touch the bucket in order to create it

                timeMeter.addTime(nanosSinceBucketCreation)
                if (!verbose) {
                    assert bucket.getAvailableTokens() == expectedTokens
                } else {
                    assert bucket.asVerbose().getAvailableTokens().value == expectedTokens
                }
            }
        }
        where:
        n | nanosSinceBucketCreation | expectedTokens |  configuration
        1 |             49           |     50         | BucketConfiguration.builder().addLimit({it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(1)}).build()
        2 |             50           |     50         | BucketConfiguration.builder().addLimit({it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)}).build()
        3 |             50           |    100         | BucketConfiguration.builder().addLimit({it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(70)}).build()
        4 |              0           |      0         | BucketConfiguration.builder().addLimit({it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)}).build()
        5 |            120           |    100         | BucketConfiguration.builder().addLimit({it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)}).build()
    }

}
