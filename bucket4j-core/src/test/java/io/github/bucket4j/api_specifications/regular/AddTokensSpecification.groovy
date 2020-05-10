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

class AddTokensSpecification extends Specification {

    @Unroll
    def "#n Add tokens spec"(
            int n, long tokensToAdd, long nanosIncrement, long requiredResult, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0)
            Bucket bucket = type.createBucket(configuration, timeMeter)
            bucket.getAvailableTokens() // touch the bucket in order to initialize

            timeMeter.addTime(nanosIncrement)
            bucket.addTokens(tokensToAdd)
            assert bucket.getAvailableTokens() == requiredResult

            if (type.isAsyncModeSupported()) {
                timeMeter = new TimeMeterMock(0)
                AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                asyncBucket.getAvailableTokens().get() // touch the bucket in order to initialize
                timeMeter.addTime(nanosIncrement)
                asyncBucket.addTokens(tokensToAdd).get()
                assert asyncBucket.getAvailableTokens().get() == requiredResult
            }
        }
        where:
        n | tokensToAdd | nanosIncrement | requiredResult | configuration
        1 |     49      |     50         |        99      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        2 |     50      |     50         |       100      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        3 |     50      |     0          |        50      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        4 |     120     |     0          |       100      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
        5 |     120     |     110        |       100      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
    }

}
