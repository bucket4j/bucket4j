package com.github.bandwidthlimiter.bucket

import com.github.bandwidthlimiter.bucket.mock.AdjusterMock
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.bandwidthlimiter.Limiters.withCustomTimePrecision
import static com.github.bandwidthlimiter.Limiters.withNanoTimePrecision
import static com.github.bandwidthlimiter.bucket.BucketExceptions.*

public class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final long VALID_PERIOD = 10;
    private static final long VALID_CAPACITY = 1000;

    @Unroll
    def "Should detect that capacity #capacity is wrong"(long capacity) {
        when:
            withNanoTimePrecision().withLimitedBandwidth(capacity, VALID_PERIOD)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == BucketExceptions.nonPositiveCapacity(capacity).message
        where:
          capacity << [-10, -5, 0]
    }

    @Unroll
    def "Should detect that initial capacity #initialCapacity is wrong"(long initialCapacity) {
        when:
            withNanoTimePrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, initialCapacity)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveInitialCapacity(initialCapacity).message
        where:
            initialCapacity << [-10, -1]
    }

    @Unroll
    def "Should check that #period is invalid period of bandwidth"(long period) {
        when:
            withNanoTimePrecision().withLimitedBandwidth(VALID_CAPACITY, period)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriod(period).message
        where:
            period << [-10, -1, 0]
    }

    def "Should check that bandwidth adjuster is not null"() {
        when:
            withNanoTimePrecision().withLimitedBandwidth(null, VALID_PERIOD, VALID_CAPACITY)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullBandwidthAdjuster().message
    }

    def "Should check than time meter is not null"() {
        setup:
            def builder = withCustomTimePrecision(null).withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD)
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullTimeMetter().message
    }

    def  "Should check that limited bandwidth list is not empty"() {
        setup:
            def builder = withNanoTimePrecision()
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == restrictionsNotSpecified().message

        when:
           builder.withGuaranteedBandwidth(VALID_CAPACITY, VALID_PERIOD).build()
        then:
            ex = thrown()
            ex.message == restrictionsNotSpecified().message
    }

    def "Should check that guaranteed capacity could not be configured twice"() {
        setup:
            def builder = withNanoTimePrecision()
                .withLimitedBandwidth(VALID_CAPACITY * 2, VALID_PERIOD)
                .withGuaranteedBandwidth(VALID_CAPACITY + 1, VALID_PERIOD)
                .withGuaranteedBandwidth(VALID_CAPACITY, VALID_PERIOD)
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == onlyOneGuarantedBandwidthSupported().message
    }

    def "Should check that guaranteed bandwidth has lesser rate than limited bandwidth"() {
        setup:
            def builder1 = withNanoTimePrecision()
                .withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD)
                .withGuaranteedBandwidth(VALID_CAPACITY, VALID_PERIOD)
            def builder2 = withNanoTimePrecision()
                .withGuaranteedBandwidth(VALID_CAPACITY, VALID_PERIOD)
                .withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD)
            def builder3 = withNanoTimePrecision()
                .withLimitedBandwidth(new AdjusterMock(VALID_CAPACITY), VALID_PERIOD, 0)
                .withGuaranteedBandwidth(VALID_CAPACITY, VALID_PERIOD)
            def builder4 = withNanoTimePrecision()
                .withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD)
                .withGuaranteedBandwidth(new AdjusterMock(VALID_CAPACITY), VALID_PERIOD, 0)
        when:
            builder1.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == guarantedHasGreaterRateThanLimited(builder1.getBandwidthDefinition(1), builder1.getBandwidthDefinition(0)).message
        when:
            builder2.build()
        then:
            ex = thrown()
            ex.message == guarantedHasGreaterRateThanLimited(builder2.getBandwidthDefinition(0), builder2.getBandwidthDefinition(1)).message
        when:
            builder3.build()
        then:
            notThrown()
        when:
            builder4.build()
        then:
            notThrown()
    }

    @Unroll
    def "Should check for overlaps, test #number"(int number, long firstCapacity, long firstPeriod, long secondCapacity, long secondPeriod) {
        setup:
            def builderWithStaticCapacity = withNanoTimePrecision()
                .withLimitedBandwidth(firstCapacity, firstPeriod)
                .withLimitedBandwidth(secondCapacity, secondPeriod)
            def builderWithDynamicCapacity1 = withNanoTimePrecision()
                .withLimitedBandwidth(new AdjusterMock(firstCapacity), firstPeriod, 0)
                .withLimitedBandwidth(secondCapacity, secondPeriod)
            def builderWithDynamicCapacity2 = withNanoTimePrecision()
                .withLimitedBandwidth(firstCapacity, firstPeriod)
                .withLimitedBandwidth(new AdjusterMock(secondCapacity), secondPeriod, 0)
        when:
            builderWithStaticCapacity.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == hasOverlaps(builderWithStaticCapacity.getBandwidthDefinition(0), builderWithStaticCapacity.getBandwidthDefinition(1)).message
        when:
            builderWithDynamicCapacity1.build()
        then:
            notThrown()
        when:
            builderWithDynamicCapacity2.build()
        then:
            notThrown()
        where:
            number |  firstCapacity | firstPeriod | secondCapacity | secondPeriod
               1   |     999        |     10      |      999       |      10
               2   |     999        |     10      |      1000      |      10
               3   |     999        |     10      |      998       |      10
               4   |     999        |     10      |      999       |      11
               5   |     999        |     10      |      999       |      9
               6   |     999        |     10      |      1000      |      8
               7   |     999        |     10      |      998       |      11
    }

    def "Should check that tokens to consume should be positive"() {
        setup:
            def bucket = withNanoTimePrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD).build()
        when:
            bucket.consume(0)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.consume(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.tryConsume(0)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.tryConsume(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.consumeAsMuchAsPossible(0)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.consumeAsMuchAsPossible(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.tryConsume(0, VALID_PERIOD)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.tryConsume(-1, VALID_PERIOD)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message
    }

    def "Should check that time units to wait should be positive"() {
        setup:
            def bucket = withNanoTimePrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD).build()
        when:
            bucket.tryConsumeSingleToken(0)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveNanosToWait(0).message

        when:
            bucket.tryConsumeSingleToken(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveNanosToWait(-1).message
    }

}
