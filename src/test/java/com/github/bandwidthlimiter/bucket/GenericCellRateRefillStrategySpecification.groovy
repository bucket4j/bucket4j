package com.github.bandwidthlimiter.bucket

import com.github.bandwidthlimiter.Limiters
import spock.lang.Specification

class GenericCellRateRefillStrategySpecification extends Specification {

    def "Specification for initialization"() {
        setup:
            def metter = new TimeMeterMock(42);
            def builder = Limiters.bucketWithCustomPrecisionPrecision(metter)
                    .withGenericCellRateRefillStrategy()
                    .withLimitedBandwidth(1000, 1000)
                    .withGuaranteedBandwidth(10, 100)
            def bucket = builder.build()
            def configuration = bucket.getConfiguration()
            def limited = configuration.getBandwidths(0)
            def guaranteed = configuration.getBandwidths(1)
        when:
            def state = bucket.createSnapshot()
        then:
            state.getCurrentSize(limited) == 1000
            state.getRefillState(configuration, 0) == 42
            state.getCurrentSize(guaranteed) == 10
            state.getRefillState(configuration, 1) == 42
    }

    def "Specification for refill"() {
        setup:
            def meter = new TimeMeterMock(10000);
            def builder = Limiters.bucketWithCustomPrecisionPrecision(meter)
                    .withGenericCellRateRefillStrategy()
                    .withLimitedBandwidth(1000, 1000, 0)
                    .withGuaranteedBandwidth(10, 100, 0)
            def bucket = builder.build()
            def configuration = bucket.getConfiguration()
            def state = bucket.createSnapshot()
            def refillStrategy = configuration.getRefillStrategy()
            def limited = configuration.getBandwidths(0)
            def guaranteed = configuration.getBandwidths(1)
        when:
            refillStrategy.refill(configuration, state, 10040)
        then:
            state.getCurrentSize(limited) == 40
            refillTime(configuration, state, limited) == 10040
            state.getCurrentSize(guaranteed) == 4
            refillTime(configuration, state, guaranteed) == 10040

        when:
            refillStrategy.refill(configuration, state, 10050)
        then:
            state.getCurrentSize(limited) == 50
            refillTime(configuration, state, limited) == 10050
            state.getCurrentSize(guaranteed) == 5
            refillTime(configuration, state, guaranteed) == 10050

        when:
            refillStrategy.refill(configuration, state, 10050)
        then:
            state.getCurrentSize(limited) == 50
            refillTime(configuration, state, limited) == 10050
            state.getCurrentSize(guaranteed) == 5
            refillTime(configuration, state, guaranteed) == 10050

        when:
            refillStrategy.refill(configuration, state, 10051)
        then:
            state.getCurrentSize(limited) == 51
            refillTime(configuration, state, limited) == 10051
            state.getCurrentSize(guaranteed) == 5
            refillTime(configuration, state, guaranteed) == 10050

        when:
            refillStrategy.refill(configuration, state, 10055)
        then:
            state.getCurrentSize(limited) == 55
            refillTime(configuration, state, limited) == 10055
            state.getCurrentSize(guaranteed) == 5
            refillTime(configuration, state, guaranteed) == 10050

        when:
            refillStrategy.refill(configuration, state, 10056)
        then:
            state.getCurrentSize(limited) == 56
            refillTime(configuration, state, limited) == 10056
            state.getCurrentSize(guaranteed) == 5
            refillTime(configuration, state, guaranteed) == 10050

        when:
            refillStrategy.refill(configuration, state, 10061)
        then:
            state.getCurrentSize(limited) == 61
            refillTime(configuration, state, limited) == 10061
            state.getCurrentSize(guaranteed) == 6
            refillTime(configuration, state, guaranteed) == 10060

        when:
            refillStrategy.refill(configuration, state, 10070)
        then:
            state.getCurrentSize(limited) == 70
            refillTime(configuration, state, limited) == 10070
            state.getCurrentSize(guaranteed) == 7
            refillTime(configuration, state, guaranteed) == 10070

        when:
            refillStrategy.refill(configuration, state, 10071)
        then:
            state.getCurrentSize(limited) == 71
            refillTime(configuration, state, limited) == 10071
            state.getCurrentSize(guaranteed) == 7
            refillTime(configuration, state, guaranteed) == 10070

        when:
            refillStrategy.refill(configuration, state, 10101)
        then:
            state.getCurrentSize(limited) == 101
            refillTime(configuration, state, limited) == 10101
            state.getCurrentSize(guaranteed) == 10
            refillTime(configuration, state, guaranteed) == 10101

        when:
            refillStrategy.refill(configuration, state, 10102)
        then:
            state.getCurrentSize(limited) == 102
            refillTime(configuration, state, limited) == 10102
            state.getCurrentSize(guaranteed) == 10
            refillTime(configuration, state, guaranteed) == 10101

        when:
            refillStrategy.refill(configuration, state, 10500)
        then:
            state.getCurrentSize(limited) == 500
            refillTime(configuration, state, limited) == 10500
            state.getCurrentSize(guaranteed) == 10
            refillTime(configuration, state, guaranteed) == 10500

        when:
            refillStrategy.refill(configuration, state, 11000)
        then:
            state.getCurrentSize(limited) == 1000
            refillTime(configuration, state, limited) == 11000
            state.getCurrentSize(guaranteed) == 10
            refillTime(configuration, state, guaranteed) == 11000

        when:
            refillStrategy.refill(configuration, state, 11003)
        then:
            state.getCurrentSize(limited) == 1000
            refillTime(configuration, state, limited) == 11003
            state.getCurrentSize(guaranteed) == 10
            refillTime(configuration, state, guaranteed) == 11000

        when:
            refillStrategy.refill(configuration, state, 20000)
        then:
            state.getCurrentSize(limited) == 1000
            refillTime(configuration, state, limited) == 20000
            state.getCurrentSize(guaranteed) == 10
            refillTime(configuration, state, guaranteed) == 20000
    }

    private long size(BucketState state, Bandwidth bandwidth) {
        return state.getCurrentSize(bandwidth)
    }

    private long refillTime(BucketConfiguration configuration, BucketState state, Bandwidth bandwidth) {
        return state.getRefillState(configuration, bandwidth.getIndexInBucket())
    }

}
