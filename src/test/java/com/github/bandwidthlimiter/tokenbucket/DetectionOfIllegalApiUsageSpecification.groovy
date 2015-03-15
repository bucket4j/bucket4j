package com.github.bandwidthlimiter.tokenbucket

import com.github.bandwidthlimiter.Limiters
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static com.github.bandwidthlimiter.Limiters.tokenBucketBuilder
import static com.github.bandwidthlimiter.tokenbucket.TokenBucketExceptions.*

public class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final TimeUnit VALID_TIMEUNIT = TimeUnit.MINUTES;
    private static final long VALID_PERIOD = 10;
    private static final long VALID_CAPACITY = 1000;

    @Unroll
    def "Should detect that capacity #capacity is wrong"(long capacity) {
        setup:
            def builder = newDefinitionBuilder().withCapacity(capacity).withInterval(VALID_PERIOD, VALID_TIMEUNIT)
        when:
            builder.buildBandwidth()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveCapacity(capacity).message
        where:
          capacity << [-10, -5, 0]
    }

    @Unroll
    def "Should detect that initial capacity #initialCapacity is wrong"(long initialCapacity) {
        setup:
            def builder = newDefinitionBuilder().withCapacity(VALID_CAPACITY)
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).withInitialCapacity(initialCapacity)
        when:
            builder.buildBandwidth()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveInitialCapacity(initialCapacity).message
        where:
            initialCapacity << [-10, -1]
    }

    def "Should check that initial capacity is equal or lesser than max capacity"() {
        setup:
            def wrongInitialCapacity = VALID_CAPACITY + 1
            def builder = newDefinitionBuilder().withCapacity(VALID_CAPACITY)
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).withInitialCapacity(wrongInitialCapacity)
        when:
            builder.buildBandwidth()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == initialCapacityGreaterThanMaxCapacity(wrongInitialCapacity, VALID_CAPACITY).message

        when:
           builder.withInitialCapacity(VALID_CAPACITY).buildBandwidth()
        then:
           notThrown IllegalArgumentException
    }

    @Unroll
    def "Should check that #period is invalid period of bandwidth"(long period) {
        setup:
            def builder = newDefinitionBuilder().withCapacity(VALID_CAPACITY).withInterval(period, VALID_TIMEUNIT);
        when:
            builder.buildBandwidth()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriod(period).message
        where:
            period << [-10, -1, 0]
    }

    def "Should check than time unit is not null"() {
        setup:
            def builder = newDefinitionBuilder().withCapacity(VALID_CAPACITY)
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).withInterval(VALID_PERIOD, null)
        when:
            builder.buildBandwidth()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullTimeUnit().message
    }

    def "Should check than refill strategy is not null"() {
        setup:
            def builder = newDefinitionBuilder().withCapacity(VALID_CAPACITY)
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).withRefillStrategy(null)
        when:
            builder.buildBandwidth()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullRefillStrategy().message
    }

    def "Should check than waiting strategy is not null"() {
        setup:
            def builder = newDefinitionBuilder().withCapacity(VALID_CAPACITY)
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).withWaitingStrategy(null)
        when:
            builder.buildBandwidth()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullWaitingStrategy().message
    }

    def "Should check that nano time wrapper is not null"() {
        setup:
            def builder = tokenBucketBuilder().withNanoTimeWrapper(null).addLimitedBandwidth()
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).withCapacity(VALID_CAPACITY);
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullNanoTimeWrapper().message
    }

    def  "Should check that limited bandwidth list is not empty"() {
        setup:
            def builder = tokenBucketBuilder()
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == restrictionsNotSpecified().message

        when:
           builder.setupGuaranteedBandwidth().withCapacity(VALID_CAPACITY).withInterval(VALID_PERIOD, VALID_TIMEUNIT).build()
        then:
            ex = thrown()
            ex.message == restrictionsNotSpecified().message
    }

    def "Should check that guaranteed capacity could not be configured twice"() {
        setup:
            def builder = tokenBucketBuilder().setupGuaranteedBandwidth().withCapacity(VALID_CAPACITY)
                    .withInterval(VALID_PERIOD, VALID_TIMEUNIT)
        when:
            builder.setupGuaranteedBandwidth()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == onlyOneGuarantedBandwidthSupported().message
    }

    private static BandwidthDefinitionBuilder newDefinitionBuilder() {
        return new BandwidthDefinitionBuilder();
    }

}
