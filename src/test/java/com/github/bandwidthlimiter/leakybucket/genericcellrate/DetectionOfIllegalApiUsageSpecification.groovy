package com.github.bandwidthlimiter.leakybucket.genericcellrate

import com.github.bandwidthlimiter.leakybucket.Bandwidth
import com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static com.github.bandwidthlimiter.BandwidthLimiters.genericCellRateBuilder
import static com.github.bandwidthlimiter.leakybucket.LeakyBucketExceptions.*
import static java.util.concurrent.TimeUnit.*

public class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final TimeUnit VALID_TIMEUNIT = MINUTES;
    private static final long VALID_PERIOD = 10;
    private static final long VALID_CAPACITY = 1000;

    @Unroll
    def "Should detect that capacity #capacity is wrong"(long capacity) {
        when:
            genericCellRateBuilder().withLimitedBandwidth(capacity, VALID_PERIOD, VALID_TIMEUNIT)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == LeakyBucketExceptions.nonPositiveCapacity(capacity).message
        where:
          capacity << [-10, -5, 0]
    }

    @Unroll
    def "Should detect that initial capacity #initialCapacity is wrong"(long initialCapacity) {
        when:
            genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT, initialCapacity)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveInitialCapacity(initialCapacity).message
        where:
            initialCapacity << [-10, -1]
    }

    def "Should check that initial capacity is equal or lesser than max capacity"() {
        when:
            long wrongInitialCapacity = VALID_CAPACITY + 1
            genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT, wrongInitialCapacity)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == initialCapacityGreaterThanMaxCapacity(wrongInitialCapacity, VALID_CAPACITY).message

        when:
            genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT, VALID_CAPACITY)
        then:
           notThrown IllegalArgumentException
    }

    @Unroll
    def "Should check that #period is invalid period of bandwidth"(long period) {
        when:
            genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, period, VALID_TIMEUNIT)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriod(period).message
        where:
            period << [-10, -1, 0]
    }

    def "Should check than time unit is not null"() {
        when:
            genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullTimeUnit().message
    }

    def "Should check than refill strategy is not null"() {
        setup:
            def builder = genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT)
        when:
            builder.withRefillStrategy(null).build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullRefillStrategy().message
    }

    def "Should check than waiting strategy is not null"() {
        setup:
            def builder = genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT)
        when:
            builder.withWaitingStrategy(null).build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullWaitingStrategy().message
    }

    def "Should check that nano time wrapper is not null"() {
        setup:
            def builder = genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT)
        when:
            builder.withNanoTimeWrapper().build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullNanoTimeWrapper().message
    }

    def  "Should check that limited bandwidth list is not empty"() {
        setup:
            def builder = genericCellRateBuilder()
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == restrictionsNotSpecified().message

        when:
           builder.withGuaranteedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT).build()
        then:
            ex = thrown()
            ex.message == restrictionsNotSpecified().message
    }

    def "Should check that guaranteed capacity could not be configured twice"() {
        setup:
            def builder = genericCellRateBuilder().withGuaranteedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT)
        when:
            builder.withGuaranteedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == onlyOneGuarantedBandwidthSupported().message
    }

    def "Should check that guaranteed bandwidth has lesser rate than limited bandwidth"() {
        setup:
            Bandwidth guaranteed = new Bandwidth(1000, 1, SECONDS)
            Bandwidth limited = new Bandwidth(1000, 1, SECONDS)
        when:
            genericCellRateBuilder().withGuaranteedBandwidth(guaranteed).withLimitedBandwidth(limited).build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == guarantedHasGreaterRateThanLimited(guaranteed, limited).message
    }

    @Unroll
    def "Should check for overlaps, test #number"(int number, Bandwidth first, Bandwidth second) {
        setup:
            def builder = genericCellRateBuilder().withLimitedBandwidth(first).withLimitedBandwidth(second)
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == hasOverlaps(first, second).message
        where:
        number |              first               |               second
           1   |  new Bandwidth(999, 10, SECONDS) | new Bandwidth(999, 10, SECONDS)
           2   |  new Bandwidth(999, 10, SECONDS) | new Bandwidth(1000, 10, SECONDS)
           3   |  new Bandwidth(999, 10, SECONDS) | new Bandwidth(998, 10, SECONDS)
           4   |  new Bandwidth(999, 10, SECONDS) | new Bandwidth(999, 11, SECONDS)
           5   |  new Bandwidth(999, 10, SECONDS) | new Bandwidth(999, 9, SECONDS)
           6   |  new Bandwidth(999, 10, SECONDS) | new Bandwidth(1000, 8, SECONDS)
           7   |  new Bandwidth(999, 10, SECONDS) | new Bandwidth(998, 11, SECONDS)
    }

    def "Should check that tokens to consume should be positive"() {
        setup:
            def tokenBucket = genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT).build()
        when:
            tokenBucket.consume(0)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            tokenBucket.consume(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message
    }

    def "Should check that nanos to wait should be positive"() {
        setup:
            def tokenBucket = genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT).build()
        when:
            tokenBucket.tryConsumeSingleToken(0)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveNanosToWait(0).message

        when:
            tokenBucket.tryConsumeSingleToken(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveNanosToWait(-1).message
    }

    def "Should check that unable try to consume number of tokens greater than smallest capacity when user has deprecated this option"() {
        setup:
            def tokenBucket = genericCellRateBuilder().raiseErrorWhenConsumeGreaterThanSmallestBandwidth()
                .withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT).build()
        when:
            tokenBucket.tryConsume(VALID_CAPACITY + 1)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == LeakyBucketExceptions.tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(VALID_CAPACITY + 1, VALID_CAPACITY).message
    }

    def "Should always check when client try to consume number of tokens greater than smallest capacity on invocation of methods with support of waiting"() {
        setup:
            def tokenBucket = genericCellRateBuilder().withLimitedBandwidth(VALID_CAPACITY, VALID_PERIOD, VALID_TIMEUNIT).build()
        when:
            tokenBucket.consume(VALID_CAPACITY + 1)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == LeakyBucketExceptions.tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(VALID_CAPACITY + 1, VALID_CAPACITY).message

        when:
            tokenBucket.tryConsume(VALID_CAPACITY + 1, 42)
        then:
            ex = thrown()
            ex.message == LeakyBucketExceptions.tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(VALID_CAPACITY + 1, VALID_CAPACITY).message
    }

}
