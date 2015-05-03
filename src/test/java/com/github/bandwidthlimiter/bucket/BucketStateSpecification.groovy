package com.github.bandwidthlimiter.bucket

import com.github.bandwidthlimiter.Buckets
import com.github.bandwidthlimiter.bucket.mock.TimeMeterMock
import spock.lang.Specification

import static java.lang.Long.MAX_VALUE


class BucketStateSpecification extends Specification {

    def "GetAvailableTokens specification"(long requiredAvailableTokens, Bucket bucket) {
        setup:
            Bandwidth[] bandwidths = bucket.configuration.bandwidths
            BucketState state = bucket.createSnapshot()
        when:
            long availableTokens = state.getAvailableTokens(bandwidths)
        then:
            availableTokens == requiredAvailableTokens
        where:
            requiredAvailableTokens |                    bucket
                    10              | Buckets.withNanoTimePrecision().withLimitedBandwidth(10, 100).build()
                     0              | Buckets.withNanoTimePrecision().withLimitedBandwidth(10, 100, 0).build()
                     5              | Buckets.withNanoTimePrecision().withLimitedBandwidth(10, 100, 5).build()
                     2              | Buckets.withNanoTimePrecision().withLimitedBandwidth(10, 100, 5).withLimitedBandwidth(2, 10).build()
                     3              | Buckets.withNanoTimePrecision().withLimitedBandwidth(10, 100, 5).withLimitedBandwidth(2, 10).withGuaranteedBandwidth(3, 1000).build()
                     2              | Buckets.withNanoTimePrecision().withLimitedBandwidth(10, 100, 5).withLimitedBandwidth(2, 10).withGuaranteedBandwidth(1, 1000).build()
    }

    def "delayAfterWillBePossibleToConsume specification"(long toConsume, long requiredTime, Bucket bucket) {
        setup:
            Bandwidth[] bandwidths = bucket.configuration.bandwidths
            BucketState state = bucket.createSnapshot()
        when:
            long actualTime = state.delayAfterWillBePossibleToConsume(bandwidths, 0, toConsume)
        then:
            actualTime == requiredTime
        where:
            toConsume | requiredTime |                               bucket
               10     |    100       | Buckets.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 100, 0).build()
                7     |     30       | Buckets.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 100, 4).build()
               11     |  MAX_VALUE   | Buckets.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 100, 4).build()
                3     |     20       | Buckets.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 100, 1).withLimitedBandwidth(5, 10, 2).build()
                3     |     20       | Buckets.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(5, 10, 2).withLimitedBandwidth(10, 100, 1).build()
                3     |      0       | Buckets.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(5, 10, 2).withGuaranteedBandwidth(10, 100, 9).build()
                6     |      0       | Buckets.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(5, 10, 2).withGuaranteedBandwidth(10, 100, 9).build()
                4     |      4       | Buckets.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(5, 10, 0).withGuaranteedBandwidth(25, 100, 3).build()
                4     |      2       | Buckets.withCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(5, 10, 3).withGuaranteedBandwidth(25, 100, 3).build()
    }

}
