
package io.github.bucket4j

import io.github.bucket4j.distributed.AsyncBucket
import io.github.bucket4j.mock.GridBackendMock
import io.github.bucket4j.mock.BucketType
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

import static io.github.bucket4j.BucketExceptions.*
import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION

class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final Duration VALID_PERIOD = Duration.ofMinutes(10)
    private static final long VALID_CAPACITY = 1000

    def builder = Bucket.builder()

    @Unroll
    def "Should detect that capacity #capacity is wrong"(long capacity) {
        when:
            builder.addLimit(Bandwidth.classic(capacity, Refill.greedy(1, VALID_PERIOD)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveCapacity(capacity).message

        where:
            capacity << [-10, -5, 0]
    }

    @Unroll
    def "Should detect that initialTokens #initialTokens is wrong"(long initialTokens) {
        when:
            Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
                    .withInitialTokens(initialTokens)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveInitialTokens(initialTokens).message
        where:
            initialTokens << [-10, -1]
    }

    @Unroll
    def "Should check that #period is invalid period of bandwidth"(long period) {
        when:
            builder.addLimit(Bandwidth.simple(VALID_CAPACITY, Duration.ofMinutes(period)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriod(Duration.ofMinutes(period).toNanos()).message

        where:
            period << [-10, -1, 0]
    }

    def "Should check that refill is not null"() {
        when:
            builder.addLimit(Bandwidth.classic(42, null))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullBandwidthRefill().message
    }

    def "Should check that bandwidth is not null"() {
        when:
            builder.addLimit(null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullBandwidth().message
    }

    def "Should check that refill period is not null"() {
        when:
            builder.addLimit(Bandwidth.classic( 32, Refill.greedy(1, null)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullRefillPeriod().message
    }

    @Unroll
    def "Should detect that refill #refillTokens tokens is invalid"(int refillTokens) {
        when:
            builder.addLimit(Bandwidth.classic( 32, Refill.greedy(refillTokens, Duration.ofSeconds(1))))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriodTokens(refillTokens).message
        where:
            refillTokens << [0, -2]
    }

    def "Should check that time meter is not null"() {
        when:
            Bucket.builder().withCustomTimePrecision(null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullTimeMeter().message
    }

    @Unroll
    def "Should check that listener is not null when decorating bucket with type #bucketType"(BucketType bucketType) {
        setup:
            def configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofMinutes(1)))
                    .build()
        when:
            bucketType.createBucket(configuration)
                    .toListenable(null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullListener().message
        where:
            bucketType << BucketType.values()
    }

    def  "Should check that limited bandwidth list is not empty"() {
        setup:
            def builder = Bucket.builder()
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == restrictionsNotSpecified().message
    }

    def "Should check that tokens to consume should be positive"() {
        setup:
            def bucket = Bucket.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()

        when:
            bucket.tryConsume(0)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.tryConsume(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.tryConsumeAsMuchAsPossible(0)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.tryConsumeAsMuchAsPossible(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.asBlocking().tryConsume(0L, VALID_PERIOD.toNanos(), BlockingStrategy.PARKING)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.asBlocking().tryConsume(-1, VALID_PERIOD.toNanos(), BlockingStrategy.PARKING)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message
    }

    def "Should detect the high rate of refill"() {
        when:
           Bucket.builder().addLimit(Bandwidth.simple(2, Duration.ofNanos(1)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == tooHighRefillRate(1, 2).message
    }

    def "Should detect the negative time of first refill"() {
        when:
            Instant timeOfFirstRefill = new Date(-10).toInstant();
            Bandwidth.classic(2, Refill.intervallyAligned(2, Duration.ofMinutes(2), timeOfFirstRefill, true))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveTimeOfFirstRefill(timeOfFirstRefill).message
    }

    def "Should prevent specification of initial tokens if intervally aligned refill used with useAdaptiveInitialTokens=true"() {
        when:
            Instant timeOfFirstRefill = Instant.now()
            Bandwidth.classic(2, Refill.intervallyAligned(2, Duration.ofMinutes(2), timeOfFirstRefill, true))
                    .withInitialTokens(1)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == intervallyAlignedRefillWithAdaptiveInitialTokensIncompatipleWithManualSpecifiedInitialTokens().message
    }

    def "Should prevent specification of nanoTime based clock if intervally aligned refill used"() {
        setup:
            Instant timeOfFirstRefill = Instant.now()
            Refill refill = Refill.intervallyAligned(2, Duration.ofMinutes(2), timeOfFirstRefill, true)
            Bandwidth bandwidth = Bandwidth.classic(2, refill)

        when:
            Bucket.builder().withNanosecondPrecision().addLimit(bandwidth).build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == intervallyAlignedRefillCompatibleOnlyWithWallClock().message

        when:
            Bucket.builder().addLimit(bandwidth).withNanosecondPrecision().build()
        then:
            ex = thrown()
            ex.message == intervallyAlignedRefillCompatibleOnlyWithWallClock().message
    }

    def "Should check that time units to wait should be positive"() {
        setup:
            def bucket = Bucket.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()
        when:
            bucket.asBlocking().tryConsume(1, 0, BlockingStrategy.PARKING)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveNanosToWait(0).message

        when:
            bucket.asScheduler().tryConsume(1, -1, BlockingStrategy.PARKING)
        then:
            ex = thrown()
            ex.message == nonPositiveNanosToWait(-1).message
    }

    @Unroll
    def "Should check that #tokens tokens is not positive to add"(long tokens) {
        setup:
            def bucket = Bucket.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()
        when:
            bucket.addTokens(tokens)
        then:
            thrown(IllegalArgumentException)
        where:
            tokens << [0, -1, -10]
    }

    def "Should that scheduler passed to tryConsume is not null"() {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD))
                    .build()
            AsyncBucket asyncBucket = BucketType.GRID.createAsyncBucket(configuration)
        when:
            asyncBucket.asScheduler().tryConsume(32, 1000_000, null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullScheduler().message
    }

    def "GridBucket should check that configuration is not null"() {
        setup:
            GridBackendMock mockProxy = new GridBackendMock(TimeMeter.SYSTEM_MILLISECONDS)
        when:
            mockProxy.builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .buildProxy("66", (BucketConfiguration) null)

        then:
            Exception ex = thrown()
            ex.message == nullConfiguration().message

        when:
            mockProxy.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildProxy("66", {null})
                    .getAvailableTokens()
        then:
            ex = thrown()
            ex.message == nullConfiguration().message

        when:
            mockProxy.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildProxy("66", (Supplier<BucketConfiguration>) null)

        then:
            ex = thrown()
            ex.message == nullConfigurationSupplier().message

        when:
            mockProxy.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildAsyncProxy("66", (BucketConfiguration) null)

        then:
            ex = thrown()
            ex.message == nullConfiguration().message

        when:
            mockProxy.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildAsyncProxy("66", {null})
                    .getAvailableTokens().get()
        then:
            ex = thrown()
            ex.cause.message == nullConfigurationFuture().message

        when:
            mockProxy.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildAsyncProxy("66", {CompletableFuture.completedFuture(null)})
                    .getAvailableTokens().get()
        then:
            ex = thrown()
            ex.cause.message == nullConfiguration().message

        when:
            mockProxy.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildAsyncProxy("66", (Supplier<CompletableFuture<BucketConfiguration>>) null)

        then:
            ex = thrown()
            ex.message == nullConfigurationSupplier().message
    }

}
