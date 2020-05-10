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

class ConsumeAsMuchAsPossibleSpecification extends Specification {

    @Unroll
    def "#n Should return #requiredResult when consumeAsMuchAsPossible tokens from Bucket #builder"(int n, long requiredResult, BucketConfiguration configuration) {
        expect:
        for (BucketType bucketType : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0)
            Bucket bucket = bucketType.createBucket(configuration, timeMeter)
            assert bucket.tryConsumeAsMuchAsPossible() == requiredResult
            if (bucketType.isAsyncModeSupported()) {
                AsyncBucket asyncBucket = bucketType.createAsyncBucket(configuration, timeMeter)
                assert asyncBucket.tryConsumeAsMuchAsPossible().get() == requiredResult
            }
        }
        where:
        n | requiredResult | configuration
        1 |        0       | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0)).build()
        2 |        2       | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(2)).build()
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consumeAsMuchAsPossible with limit #limit tokens from Bucket #configuration"(
            int n, long requiredResult, long limit, BucketConfiguration configuration) {
        expect:
        for (BucketType bucketType : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0)
            Bucket bucket = bucketType.createBucket(configuration, timeMeter)
            assert bucket.tryConsumeAsMuchAsPossible(limit) == requiredResult
            if (bucketType.isAsyncModeSupported()) {
                AsyncBucket asyncBucket = bucketType.createAsyncBucket(configuration, timeMeter)
                assert asyncBucket.tryConsumeAsMuchAsPossible(limit).get() == requiredResult
            }
        }
        where:
        n | requiredResult |   limit   | configuration
        1 |       4        |     5     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(4)).build()
        2 |       5        |     5     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5)).build()
        3 |       5        |     5     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5)).build()
    }

}
