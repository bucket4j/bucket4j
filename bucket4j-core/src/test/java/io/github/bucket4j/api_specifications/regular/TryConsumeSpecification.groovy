package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucket
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class TryConsumeSpecification extends Specification {

    @Unroll
    def "#n Should return #requiredResult when trying to consume #toConsume tokens from Bucket #builder"(
            int n, boolean requiredResult, long toConsume, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            def timeMeter = new TimeMeterMock(0)
            Bucket bucket = type.createBucket(configuration, timeMeter)
            assert bucket.tryConsume(toConsume) == requiredResult
            if (type.isAsyncModeSupported()) {
                AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                assert asyncBucket.tryConsume(toConsume).get() == requiredResult
            }
        }
        where:
        n | requiredResult | toConsume | configuration
        1 |     false      |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0)).build()
        2 |      true      |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(1)).build()
    }

}
