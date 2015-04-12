package com.github.bandwidthlimiter.bucket

import com.github.bandwidthlimiter.Limiters
import spock.lang.Specification

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

//    def "Specification for refill"() {
//        setup:
//            def meter = new TimeMeterMock(10000);
//            def builder = Limiters.withCustomTimePrecision(meter)
//                    .withGenericCellRateRefillStrategy()
//                    .withLimitedBandwidth(1000, 1000, 0)
//                    .withGuaranteedBandwidth(10, 100, 0)
//            def bucket = builder.buildLocalThreadSafe()
//            def configuration = bucket.getConfiguration()
//            def state = bucket.createSnapshot()
//            def refillStrategy = configuration.getRefillStrategy()
//            def limited = configuration.getBandwidths(0)
//            def guaranteed = configuration.getBandwidths(1)
//        when:
//            refillStrategy.refill(configuration, state, 10040)
//        then:
//            state.getCurrentSize(limited) == 40
//            refillTime(configuration, state, limited) == 10040
//            state.getCurrentSize(guaranteed) == 4
//            refillTime(configuration, state, guaranteed) == 10040
//
//        when:
//            refillStrategy.refill(configuration, state, 10050)
//        then:
//            state.getCurrentSize(limited) == 50
//            refillTime(configuration, state, limited) == 10050
//            state.getCurrentSize(guaranteed) == 5
//            refillTime(configuration, state, guaranteed) == 10050
//
//        when:
//            refillStrategy.refill(configuration, state, 10050)
//        then:
//            state.getCurrentSize(limited) == 50
//            refillTime(configuration, state, limited) == 10050
//            state.getCurrentSize(guaranteed) == 5
//            refillTime(configuration, state, guaranteed) == 10050
//
//        when:
//            refillStrategy.refill(configuration, state, 10051)
//        then:
//            state.getCurrentSize(limited) == 51
//            refillTime(configuration, state, limited) == 10051
//            state.getCurrentSize(guaranteed) == 5
//            refillTime(configuration, state, guaranteed) == 10050
//
//        when:
//            refillStrategy.refill(configuration, state, 10055)
//        then:
//            state.getCurrentSize(limited) == 55
//            refillTime(configuration, state, limited) == 10055
//            state.getCurrentSize(guaranteed) == 5
//            refillTime(configuration, state, guaranteed) == 10050
//
//        when:
//            refillStrategy.refill(configuration, state, 10056)
//        then:
//            state.getCurrentSize(limited) == 56
//            refillTime(configuration, state, limited) == 10056
//            state.getCurrentSize(guaranteed) == 5
//            refillTime(configuration, state, guaranteed) == 10050
//
//        when:
//            refillStrategy.refill(configuration, state, 10061)
//        then:
//            state.getCurrentSize(limited) == 61
//            refillTime(configuration, state, limited) == 10061
//            state.getCurrentSize(guaranteed) == 6
//            refillTime(configuration, state, guaranteed) == 10060
//
//        when:
//            refillStrategy.refill(configuration, state, 10070)
//        then:
//            state.getCurrentSize(limited) == 70
//            refillTime(configuration, state, limited) == 10070
//            state.getCurrentSize(guaranteed) == 7
//            refillTime(configuration, state, guaranteed) == 10070
//
//        when:
//            refillStrategy.refill(configuration, state, 10071)
//        then:
//            state.getCurrentSize(limited) == 71
//            refillTime(configuration, state, limited) == 10071
//            state.getCurrentSize(guaranteed) == 7
//            refillTime(configuration, state, guaranteed) == 10070
//
//        when:
//            refillStrategy.refill(configuration, state, 10101)
//        then:
//            state.getCurrentSize(limited) == 101
//            refillTime(configuration, state, limited) == 10101
//            state.getCurrentSize(guaranteed) == 10
//            refillTime(configuration, state, guaranteed) == 10101
//
//        when:
//            refillStrategy.refill(configuration, state, 10102)
//        then:
//            state.getCurrentSize(limited) == 102
//            refillTime(configuration, state, limited) == 10102
//            state.getCurrentSize(guaranteed) == 10
//            refillTime(configuration, state, guaranteed) == 10101
//
//        when:
//            refillStrategy.refill(configuration, state, 10500)
//        then:
//            state.getCurrentSize(limited) == 500
//            refillTime(configuration, state, limited) == 10500
//            state.getCurrentSize(guaranteed) == 10
//            refillTime(configuration, state, guaranteed) == 10500
//
//        when:
//            refillStrategy.refill(configuration, state, 11000)
//        then:
//            state.getCurrentSize(limited) == 1000
//            refillTime(configuration, state, limited) == 11000
//            state.getCurrentSize(guaranteed) == 10
//            refillTime(configuration, state, guaranteed) == 11000
//
//        when:
//            refillStrategy.refill(configuration, state, 11003)
//        then:
//            state.getCurrentSize(limited) == 1000
//            refillTime(configuration, state, limited) == 11003
//            state.getCurrentSize(guaranteed) == 10
//            refillTime(configuration, state, guaranteed) == 11000
//
//        when:
//            refillStrategy.refill(configuration, state, 20000)
//        then:
//            state.getCurrentSize(limited) == 1000
//            refillTime(configuration, state, limited) == 20000
//            state.getCurrentSize(guaranteed) == 10
//            refillTime(configuration, state, guaranteed) == 20000
//    }
//
//    def "spec for timeRequiredToRefill"() {
//        when:
//            def bucket = Limiters.withMillisTimePrecision()
//                .withLimitedBandwidth(100, 1000)
//                .withLimitedBandwidth(10, 10).buildLocalUnsafe()
//            def configuration = bucket.getConfiguration()
//            def bandwidth0 = configuration.getBandwidths()[0]
//            def bandwidth1 = configuration.getBandwidths()[1]
//            def refillStrategy = bucket.getConfiguration().getRefillStrategy()
//        then:
//            refillStrategy.timeRequiredToRefill(configuration, bandwidth0, 100) == 1000
//            refillStrategy.timeRequiredToRefill(configuration, bandwidth0, 30) == 300
//
//            refillStrategy.timeRequiredToRefill(configuration, bandwidth1, 10) == 10
//            refillStrategy.timeRequiredToRefill(configuration, bandwidth1, 6) == 6
//    }

    private long size(BucketState state, Bandwidth bandwidth) {
        return state.getCurrentSize(bandwidth)
    }

    private long refillTime(BucketConfiguration configuration, BucketState state, Bandwidth bandwidth) {
        return state.getRefillState(configuration, bandwidth.getIndexInBucket())
    }

}
