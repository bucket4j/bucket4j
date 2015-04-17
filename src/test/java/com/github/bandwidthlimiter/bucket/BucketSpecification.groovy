package com.github.bandwidthlimiter.bucket

import com.github.bandwidthlimiter.Limiters
import com.github.bandwidthlimiter.bucket.mock.BucketType
import com.github.bandwidthlimiter.bucket.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

class BucketSpecification extends Specification {

    @Unroll
    def "#n Should return #requiredResult when trying to consume #toConsume tokens from Bucket #builder"(
            int n, boolean requiredResult, long toConsume, BucketBuilder builder) {
        expect:
            for (Bucket bucket : BucketType.createBuckets(builder)) {
                assert bucket.tryConsume(toConsume) == requiredResult
            }
        where:
            n | requiredResult | toConsume | builder
            1 |     false       |     1     | Limiters.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 100, 0)
            2 |      true       |     1     | Limiters.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 100, 1)
    }


}
