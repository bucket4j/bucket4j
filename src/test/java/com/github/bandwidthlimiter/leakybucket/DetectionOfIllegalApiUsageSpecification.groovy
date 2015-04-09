package com.github.bandwidthlimiter.leakybucket

import spock.lang.Specification
import spock.lang.Unroll


import static com.github.bandwidthlimiter.BandwidthLimiters.leakyBucketWithNanoPrecision
import static com.github.bandwidthlimiter.BandwidthLimiters.leakyBucketWithCustomPrecisionPrecision
import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.*

public class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final long VALID_PERIOD = 10;
    private static final long VALID_CAPACITY = 1000;

    @Unroll
    def "Should detect that capacity #capacity is wrong"(long capacity) {
        when:
            leakyBucketWithNanoPrecision().withLimitedBandwidth(capacity, VALID_PERIOD)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == LeakyBucketExceptions.nonPositiveCapacity(capacity).message
        where:
          capacity << [-10, -5, 0]
    }

    @Unroll
    def "Should detect that initial capacity #initialCapacity is wrong"(long initialCapacity) {
        when:
            leakyBucketWithNanoPrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, initialCapacity)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveInitialCapacity(initialCapacity).message
        where:
            initialCapacity << [-10, -1]
    }

    def "Should check that initial capacity is equal or lesser than max capacity"() {
        setup:
            long wrongInitialCapacity = VALID_CAPACITY + 1
        when:
            leakyBucketWithNanoPrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, wrongInitialCapacity)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == initialCapacityGreaterThanMaxCapacity(wrongInitialCapacity, VALID_CAPACITY).message

        when:
            leakyBucketWithNanoPrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_CAPACITY)
        then:
           notThrown IllegalArgumentException
    }

    @Unroll
    def "Should check that #period is invalid period of bandwidth"(long period) {
        when:
            leakyBucketWithNanoPrecision().withLimitedBandwidth(VALID_CAPACITY, period)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriod(period).message
        where:
            period << [-10, -1, 0]
    }

    def "Should check than time metter is not null"() {
        setup:
            def builder = leakyBucketWithCustomPrecisionPrecision(null).withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD)
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullTimeMetter().message
    }

    def "Should check than refill strategy is not null"() {
        setup:
            def builder = leakyBucketWithNanoPrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD)
        when:
            builder.withCustomRefillStrategy(null).build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullRefillStrategy().message
    }

    def  "Should check that limited bandwidth list is not empty"() {
        setup:
            def builder = leakyBucketWithNanoPrecision()
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
            def builder = leakyBucketWithNanoPrecision()
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
            def builder = leakyBucketWithNanoPrecision()
                .withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD)
                .withGuaranteedBandwidth(VALID_CAPACITY, VALID_PERIOD)
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == guarantedHasGreaterRateThanLimited(builder.getBandwidth(1), builder.getBandwidth(0)).message
    }

    @Unroll
    def "Should check for overlaps, test #number"(int number, long firstCapacity, long firstPeriod, long secondCapacity, long secondPeriod) {
        setup:
            def builder = leakyBucketWithNanoPrecision()
                .withLimitedBandwidth(firstCapacity, firstPeriod)
                .withLimitedBandwidth(secondCapacity, secondPeriod)
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == hasOverlaps(builder.getBandwidth(0), builder.getBandwidth(1)).message
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
            def bucket = leakyBucketWithNanoPrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD).build()
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
    }

    def "Should check that time units to wait should be positive"() {
        setup:
            def bucket = leakyBucketWithNanoPrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD).build()
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

    def "Should check that unable try to consume number of tokens greater than smallest capacity when user has deprecated this option"() {
        setup:
            def bucket = leakyBucketWithNanoPrecision()
                .raiseErrorWhenConsumeGreaterThanSmallestBandwidth()
                .withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD).build()
        when:
            bucket.tryConsume(VALID_CAPACITY + 1)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == LeakyBucketExceptions.tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(VALID_CAPACITY + 1, VALID_CAPACITY).message
    }

    def "Should always check when client try to consume number of tokens greater than smallest capacity on invocation of methods with support of waiting"() {
        setup:
            def bucket = leakyBucketWithNanoPrecision().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD).build()
        when:
            bucket.consume(VALID_CAPACITY + 1)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(VALID_CAPACITY + 1, VALID_CAPACITY).message

        when:
            bucket.tryConsume(VALID_CAPACITY + 1, 42)
        then:
            ex = thrown()
            ex.message == tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(VALID_CAPACITY + 1, VALID_CAPACITY).message
    }

}
