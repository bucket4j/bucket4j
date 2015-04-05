package com.github.bandwidthlimiter.tokenbucket

import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static com.github.bandwidthlimiter.TokenBuckets.tokenBucketBuilder
import static com.github.bandwidthlimiter.tokenbucket.TokenBucketExceptions.*
import static java.util.concurrent.TimeUnit.*

public class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final TimeUnit VALID_TIMEUNIT = MINUTES;
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

    def "Should check that guaranteed bandwidth has lesser rate than limited bandwidth"() {
        setup:
            BandwidthDefinitionBuilder guaranteed = newDefinitionBuilder().withCapacity(1000).withInterval(1, SECONDS)
            BandwidthDefinitionBuilder limited = newDefinitionBuilder().withCapacity(999).withInterval(1, SECONDS)
            def builder = tokenBucketBuilder().setGuaranteedBandwidth(guaranteed).addLimitedBandwidth(limited)
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == guarantedHasGreaterRateThanLimited(guaranteed.buildBandwidth(), limited.buildBandwidth()).message
    }

    @Unroll
    def "Should check for overlaps, test #number"(int number, BandwidthDefinitionBuilder first, BandwidthDefinitionBuilder second) {
        setup:
            def builder = tokenBucketBuilder().addLimitedBandwidth(first).addLimitedBandwidth(second)
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == hasOverlaps(first.buildBandwidth(), second.buildBandwidth()).message
        where:
        number |                     first                                           |                         second
           1   |  newDefinitionBuilder().withCapacity(999).withInterval(10, SECONDS) | newDefinitionBuilder().withCapacity(999).withInterval(10, SECONDS)
           2   |  newDefinitionBuilder().withCapacity(999).withInterval(10, SECONDS) | newDefinitionBuilder().withCapacity(1000).withInterval(10, SECONDS)
           3   |  newDefinitionBuilder().withCapacity(999).withInterval(10, SECONDS) | newDefinitionBuilder().withCapacity(998).withInterval(10, SECONDS)
           4   |  newDefinitionBuilder().withCapacity(999).withInterval(10, SECONDS) | newDefinitionBuilder().withCapacity(999).withInterval(11, SECONDS)
           5   |  newDefinitionBuilder().withCapacity(999).withInterval(10, SECONDS) | newDefinitionBuilder().withCapacity(999).withInterval(9, SECONDS)
           6   |  newDefinitionBuilder().withCapacity(999).withInterval(10, SECONDS) | newDefinitionBuilder().withCapacity(1000).withInterval(8, SECONDS)
           7   |  newDefinitionBuilder().withCapacity(999).withInterval(10, SECONDS) | newDefinitionBuilder().withCapacity(998).withInterval(11, SECONDS)
    }

    def "Should check that tokens to consume should be positive"() {
        setup:
            def tokenBucket = tokenBucketBuilder().addLimitedBandwidth().withCapacity(VALID_CAPACITY)
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).build()
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
            def tokenBucket = tokenBucketBuilder().addLimitedBandwidth().withCapacity(VALID_CAPACITY)
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).build()
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
            def tokenBucket = tokenBucketBuilder().raiseErrorWhenConsumeGreaterThanSmallestBandwidth()
                .addLimitedBandwidth().withCapacity(VALID_CAPACITY)
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).build()
        when:
            tokenBucket.tryConsume(VALID_CAPACITY + 1)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == TokenBucketExceptions.tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(VALID_CAPACITY + 1, VALID_CAPACITY).message
    }

    def "Should always check when client try to consume number of tokens greater than smallest capacity on invocation of methods with support of waiting"() {
        setup:
            def tokenBucket = tokenBucketBuilder().addLimitedBandwidth().withCapacity(VALID_CAPACITY)
                .withInterval(VALID_PERIOD, VALID_TIMEUNIT).build()
        when:
            tokenBucket.consume(VALID_CAPACITY + 1)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == TokenBucketExceptions.tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(VALID_CAPACITY + 1, VALID_CAPACITY).message

        when:
            tokenBucket.tryConsume(VALID_CAPACITY + 1, 42)
        then:
            ex = thrown()
            ex.message == TokenBucketExceptions.tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(VALID_CAPACITY + 1, VALID_CAPACITY).message
    }

    private static BandwidthDefinitionBuilder newDefinitionBuilder() {
        return new BandwidthDefinitionBuilder();
    }

}
