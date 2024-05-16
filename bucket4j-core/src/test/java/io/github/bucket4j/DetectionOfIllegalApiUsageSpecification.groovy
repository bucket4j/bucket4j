
package io.github.bucket4j


import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.ProxyManagerMock
import io.github.bucket4j.mock.BucketType
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

import static io.github.bucket4j.BucketExceptions.*
import static java.time.Duration.ofMinutes
import static java.time.Duration.ofNanos

class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final Duration VALID_PERIOD = ofMinutes(10)
    private static final long VALID_CAPACITY = 1000

    def builder = Bucket.builder()

    @Unroll
    def "Should detect that capacity #capacity is wrong"(long capacity) {
        when:
            builder.addLimit({limit -> limit.capacity(capacity).refillGreedy(1, VALID_PERIOD)})
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveCapacity(capacity).message

        where:
            capacity << [-10, -5, 0]
    }

    @Unroll
    def "Should detect that initialTokens #initialTokens is wrong"(long initialTokens) {
        when:
            Bucket.builder()
                .addLimit({
                    it.capacity(VALID_CAPACITY).refillGreedy(VALID_CAPACITY, VALID_PERIOD)
                            .initialTokens(initialTokens)})
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveInitialTokens(initialTokens).message
        where:
            initialTokens << [-10, -1]
    }

    @Unroll
    def "Should check that #period is invalid period of bandwidth"(long period) {
        when:
            builder.addLimit({limit -> limit.capacity(VALID_CAPACITY).refillGreedy(VALID_CAPACITY, ofMinutes(period))})
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriod(ofMinutes(period).toNanos()).message

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
            builder.addLimit((Bandwidth) null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullBandwidth().message
    }

    def "Should check that bandwidth builder is not null"() {
        when:
            builder.addLimit(null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullBuilder().message

        when:
            BucketConfiguration.builder().addLimit(null)
        then:
            IllegalArgumentException ex2 = thrown()
            ex2.message == nullBuilder().message
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
            builder.addLimit({limit -> limit.capacity(32).refillGreedy(refillTokens, Duration.ofSeconds(1))} )
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
                    .addLimit({limit -> limit.capacity(3).refillGreedy(3, ofMinutes(1))})
                    .build()
        when:
            bucketType.createBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS).toListenable(null)
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
            def bucket = Bucket.builder().addLimit({
                limit -> limit.capacity(3).refillGreedy(3, ofMinutes(1))
            }).build()

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
           Bucket.builder().addLimit(Bandwidth.simple(2, ofNanos(1)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == tooHighRefillRate(1, 2).message
    }

    def "Should detect the negative time of first refill"() {
        when:
            Instant timeOfFirstRefill = new Date(-10).toInstant()
            Bandwidth.builder().capacity(2).refillIntervallyAlignedWithAdaptiveInitialTokens(2, ofMinutes(2), timeOfFirstRefill).build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveTimeOfFirstRefill(timeOfFirstRefill).message
    }

    def "Should prevent specification of initial tokens if intervally aligned refill used with useAdaptiveInitialTokens=true"() {
        when:
            Instant timeOfFirstRefill = Instant.now()
            Bandwidth.builder()
                .capacity(2)
                .refillIntervallyAlignedWithAdaptiveInitialTokens(2, ofMinutes(2), timeOfFirstRefill)
                .initialTokens(1)
                .build()

        then:
            IllegalArgumentException ex = thrown()
            ex.message == intervallyAlignedRefillWithAdaptiveInitialTokensIncompatipleWithManualSpecifiedInitialTokens().message
    }

    def "Should prevent specification of nanoTime based clock if intervally aligned refill used"() {
        setup:
            Bandwidth bandwidth = Bandwidth.builder()
                .capacity(40)
                .refillIntervallyAlignedWithAdaptiveInitialTokens(300, Duration.ofSeconds(4200), Instant.now())
                .id("123")
                .build()
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

    @Unroll
    def "#type should detect that all bandwidth has unique id"(BucketType type) {
        when:
            Bucket.builder()
                .addLimit(Bandwidth.simple(1, Duration.ofSeconds(10)).withId("xyz"))
                .addLimit(Bandwidth.simple(100, Duration.ofSeconds(3600)).withId("xyz"))
                .build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == foundTwoBandwidthsWithSameId(0, 1, "xyz").message
        where:
            type << BucketType.values()
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
            bucket.asBlocking().tryConsume(1, -1, BlockingStrategy.PARKING)
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

    @Unroll
    def "Should check that #tokens tokens is not positive to force add"(long tokens) {
        setup:
            def bucket = Bucket.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()
        when:
            bucket.forceAddTokens(tokens)
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
            AsyncBucketProxy asyncBucket = BucketType.GRID.createAsyncBucket(configuration)
        when:
            asyncBucket.asScheduler().tryConsume(32, 1000_000, null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullScheduler().message
    }

    def "GridBucket should check that configuration is not null"() {
        setup:
            ProxyManagerMock mockProxy = new ProxyManagerMock(TimeMeter.SYSTEM_MILLISECONDS)
        when:
            mockProxy.builder()
                .build("66", { null })
                .getAvailableTokens()
        then:
            Exception ex = thrown()
            ex.message == nullConfiguration().message

        when:
            mockProxy.builder()
                .build("66", (Supplier<BucketConfiguration>) null)

        then:
            ex = thrown()
            ex.message == nullConfigurationSupplier().message

        when:
            mockProxy.asAsync().builder()
                .build("66", { null })
                .getAvailableTokens().get()
        then:
            ex = thrown()
            ex.cause.message == nullConfigurationFuture().message

        when:
            mockProxy.asAsync().builder()
                .build("66", { CompletableFuture.completedFuture(null) })
                .getAvailableTokens().get()
        then:
            ex = thrown()
            ex.cause.message == nullConfiguration().message

        when:
            mockProxy.asAsync().builder()
                .build("66", (Supplier<CompletableFuture<BucketConfiguration>>) null)

        then:
            ex = thrown()
            ex.message == nullConfigurationSupplier().message
    }

    @Unroll
    def "#type should detect that configuration is null during configuration replacement"(BucketType type) {
        setup:
            def configuration = BucketConfiguration.builder().addLimit(Bandwidth.simple(1, Duration.ofSeconds(10))).build()
            def bucket = type.createBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS)
            def asyncBucket = type.createAsyncBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS)

        when:
            bucket.replaceConfiguration(null, TokensInheritanceStrategy.AS_IS)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullConfiguration().message

        when:
            bucket.asVerbose().replaceConfiguration(null, TokensInheritanceStrategy.AS_IS)
        then:
            ex = thrown()
            ex.message == nullConfiguration().message

        when:
            asyncBucket.replaceConfiguration(null, TokensInheritanceStrategy.AS_IS)
        then:
            ex = thrown()
            ex.message == nullConfiguration().message

        when:
            asyncBucket.asVerbose().replaceConfiguration(null, TokensInheritanceStrategy.AS_IS)
        then:
            ex = thrown()
            ex.message == nullConfiguration().message

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type should detect that tokenMigrationMode is null during configuration replacement"(BucketType type) {
        setup:
            def builder = BucketConfiguration.builder().addLimit(Bandwidth.simple(1, Duration.ofSeconds(10)))
            def bucket = type.createBucket(builder.build(), TimeMeter.SYSTEM_MILLISECONDS)
            def asyncBucket = type.createAsyncBucket(builder.build(), TimeMeter.SYSTEM_MILLISECONDS)
            def newConfiguration = builder.build()
        when:
            bucket.replaceConfiguration(newConfiguration, null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullTokensInheritanceStrategy().message

        when:
            bucket.asVerbose().replaceConfiguration(newConfiguration, null)
        then:
            ex = thrown()
            ex.message == nullTokensInheritanceStrategy().message

        when:
            asyncBucket.replaceConfiguration(newConfiguration, null)
        then:
            ex = thrown()
            ex.message == nullTokensInheritanceStrategy().message

        when:
            asyncBucket.asVerbose().replaceConfiguration(newConfiguration, null)
        then:
            ex = thrown()
            ex.message == nullTokensInheritanceStrategy().message

        where:
            type << BucketType.values()
    }

}
