package com.github.bandwidthlimiter.bucket

import com.github.bandwidthlimiter.Limiters
import spock.lang.Specification
import spock.lang.Unroll

class BandwidthSpecification extends Specification {

    def "Specification for initialization"(long period, long capacity, long initialCapacity, long currentTime) {
        setup:
            def meter = new TimeMeterMock(currentTime);
            def builder = Limiters.withCustomTimePrecision(meter)
                .withLimitedBandwidth(capacity, period, initialCapacity)
            def bucket = builder.buildLocalThreadSafe()
            def bandwidth = bucket.getConfiguration().getBandwidth(0)
        when:
            def state = bucket.createSnapshot()
        then:
            bandwidth.getCurrentSize(state) == initialCapacity
            bandwidth.getMaxCapacity(state) == capacity
            bandwidth.getRefillTime(state) == currentTime
        where:
            period | capacity | initialCapacity | currentTime
              10   |   100    |      50         |    10000
              10   |    70    |      80         |    10000
    }

    @Unroll
    def "Specification for refill #n"(int n, long initialCapacity, long period, long initTime,
               long maxCapacityBefore, long maxCapacityAfter,
               long timeRefill, long requiredSize, long requiredTimeRefill) {
        setup:
            def meter = new TimeMeterMock(initTime);
            def adjuster = new AdjusterMock(maxCapacityBefore)
            def bucket = Limiters.withCustomTimePrecision(meter)
                    .withLimitedBandwidth(adjuster, period, initialCapacity)
                    .buildLocalUnsafe()
            def bandwidth = bucket.getConfiguration().getBandwidth(0)
            def state = bucket.createSnapshot()
        when:
            adjuster.setCapacity(maxCapacityAfter)
            meter.setCurrentTime(timeRefill)
            bandwidth.refill(state, timeRefill)
        then:
            bandwidth.getCurrentSize(state) == requiredSize
            bandwidth.getRefillTime(state) == requiredTimeRefill
            bandwidth.getMaxCapacity(state) == maxCapacityAfter
        where:
            n  | initialCapacity | period | initTime | maxCapacityBefore | maxCapacityAfter | timeRefill | requiredSize | requiredTimeRefill
//            1  |      0          | 1000   | 10000    | 1000              | 1000             | 10040      | 40           | 10040
//            2  |      0          | 100    | 10000    | 10                | 10               | 10040      | 4            | 10040
//            3  |     40          | 1000   | 10040    | 1000              | 1000             | 10050      | 50           | 10050
//            4  |      4          | 100    | 10040    | 10                | 10               | 10050      | 5            | 10050
//            5  |     50          | 1000   | 10050    | 1000              | 1000             | 10050      | 50           | 10050
//            6  |      5          | 100    | 10050    | 10                | 10               | 10050      | 5            | 10050
//            7  |     50          | 1000   | 10050    | 1000              | 1000             | 10051      | 51           | 10051
//            8  |      5          | 100    | 10050    | 10                | 10               | 10051      | 5            | 10050
//            9  |     51          | 1000   | 10051    | 1000              | 1000             | 10055      | 55           | 10055
//            10 |      5          | 100    | 10050    | 10                | 10               | 10055      | 5            | 10050
//            11 |     55          | 1000   | 10055    | 1000              | 1000             | 10056      | 56           | 10056
//            12 |      5          | 100    | 10050    | 10                | 10               | 10055      | 5            | 10050
//            13 |     56          | 1000   | 10056    | 1000              | 1000             | 10061      | 61           | 10061
//            14 |      5          | 100    | 10050    | 10                | 10               | 10061      | 6            | 10060
//            15 |     61          | 1000   | 10061    | 1000              | 1000             | 10070      | 70           | 10070
//            16 |      6          | 100    | 10060    | 10                | 10               | 10070      | 7            | 10070
//            17 |     70          | 1000   | 10070    | 1000              | 1000             | 10071      | 71           | 10071
//            18 |      7          | 100    | 10070    | 10                | 10               | 10071      | 7            | 10070
//            19 |     71          | 1000   | 10071    | 1000              | 1000             | 10101      | 101          | 10101
//            20 |      7          | 100    | 10070    | 10                | 10               | 10101      | 10           | 10101
//            21 |    101          | 1000   | 10101    | 1000              | 1000             | 10102      | 102          | 10102
//            22 |     10          | 100    | 10101    | 10                | 10               | 10102      | 10           | 10102
//            23 |    102          | 1000   | 10102    | 1000              | 1000             | 10500      | 500          | 10500
//            24 |     10          | 100    | 10102    | 10                | 10               | 10500      | 10           | 10500
//            25 |    500          | 1000   | 10500    | 1000              | 1000             | 11000      | 1000         | 11000
//            26 |     10          | 100    | 10500    | 10                | 10               | 11000      | 10           | 11000
//            27 |   1000          | 1000   | 11000    | 1000              | 1000             | 11003      | 1000         | 11003
//            28 |     10          | 100    | 11000    | 10                | 10               | 11003      | 10           | 11003
//            29 |   1000          | 1000   | 11003    | 1000              | 1000             | 20000      | 1000         | 20000
//            30 |     10          | 100    | 11003    | 10                | 10               | 20000      | 10           | 20000

            31 |   1000          | 1000   | 20000    | 1000              | 1100             | 20004      | 1005         | 20004
//            32 |     10          | 100    | 20000    | 10                | 11               | 20005      | 10           | 20000
    }

    private long size(BucketState state, Bandwidth bandwidth) {
        return state.getCurrentSize(bandwidth)
    }

    private long refillTime(BucketConfiguration configuration, BucketState state, Bandwidth bandwidth) {
        return state.getRefillState(configuration, bandwidth.getIndexInBucket())
    }

}
