package io.github.bucket4j;

import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.ProxyManagerMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.github.bucket4j.BucketExceptions.foundTwoBandwidthsWithSameId;
import static io.github.bucket4j.BucketExceptions.intervallyAlignedRefillCompatibleOnlyWithWallClock;
import static io.github.bucket4j.BucketExceptions.intervallyAlignedRefillWithAdaptiveInitialTokensIncompatipleWithManualSpecifiedInitialTokens;
import static io.github.bucket4j.BucketExceptions.nonPositiveCapacity;
import static io.github.bucket4j.BucketExceptions.nonPositiveInitialTokens;
import static io.github.bucket4j.BucketExceptions.nonPositiveNanosToWait;
import static io.github.bucket4j.BucketExceptions.nonPositivePeriod;
import static io.github.bucket4j.BucketExceptions.nonPositivePeriodTokens;
import static io.github.bucket4j.BucketExceptions.nonPositiveTimeOfFirstRefill;
import static io.github.bucket4j.BucketExceptions.nonPositiveTokensToConsume;
import static io.github.bucket4j.BucketExceptions.nullBandwidth;
import static io.github.bucket4j.BucketExceptions.nullBandwidthRefill;
import static io.github.bucket4j.BucketExceptions.nullBuilder;
import static io.github.bucket4j.BucketExceptions.nullConfiguration;
import static io.github.bucket4j.BucketExceptions.nullConfigurationFuture;
import static io.github.bucket4j.BucketExceptions.nullConfigurationSupplier;
import static io.github.bucket4j.BucketExceptions.nullListener;
import static io.github.bucket4j.BucketExceptions.nullRefillPeriod;
import static io.github.bucket4j.BucketExceptions.nullScheduler;
import static io.github.bucket4j.BucketExceptions.nullTimeMeter;
import static io.github.bucket4j.BucketExceptions.nullTokensInheritanceStrategy;
import static io.github.bucket4j.BucketExceptions.restrictionsNotSpecified;
import static io.github.bucket4j.BucketExceptions.tooHighRefillRate;
import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofNanos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DetectionOfIllegalApiUsage Specification")
class DetectionOfIllegalApiUsageTest {

    private static final Duration VALID_PERIOD = ofMinutes(10);
    private static final long VALID_CAPACITY = 1000;

    private final LocalBucketBuilder builder = Bucket.builder();

    private record CapacityCase(long capacity) {

        @Override
        public String toString() {
            return "capacity=" + capacity;
        }
    }

    private record InitialTokensCase(long initialTokens) {

        @Override
        public String toString() {
            return "initialTokens=" + initialTokens;
        }
    }

    private record PeriodCase(long period) {

        @Override
        public String toString() {
            return "period=" + period;
        }
    }

    private record RefillTokensCase(int refillTokens) {

        @Override
        public String toString() {
            return "refillTokens=" + refillTokens;
        }
    }

    private record BucketTypeCase(BucketType bucketType) {

        @Override
        public String toString() {
            return "bucketType=" + bucketType;
        }
    }

    private record TokensCase(long tokens) {

        @Override
        public String toString() {
            return "tokens=" + tokens;
        }
    }

    static Stream<CapacityCase> wrongCapacities() {
        return Stream.of(new CapacityCase(-10), new CapacityCase(-5), new CapacityCase(0));
    }

    static Stream<InitialTokensCase> wrongInitialTokens() {
        return Stream.of(new InitialTokensCase(-10), new InitialTokensCase(-1));
    }

    static Stream<PeriodCase> invalidPeriods() {
        return Stream.of(new PeriodCase(-10), new PeriodCase(-1), new PeriodCase(0));
    }

    static Stream<RefillTokensCase> invalidRefillTokens() {
        return Stream.of(new RefillTokensCase(0), new RefillTokensCase(-2));
    }

    static Stream<BucketTypeCase> bucketTypes() {
        return Stream.of(BucketType.values()).map(BucketTypeCase::new);
    }

    static Stream<TokensCase> nonPositiveTokensToAdd() {
        return Stream.of(new TokensCase(0), new TokensCase(-1), new TokensCase(-10));
    }

    static Stream<TokensCase> nonPositiveTokensToForceAdd() {
        return Stream.of(new TokensCase(0), new TokensCase(-1), new TokensCase(-10));
    }

    @ParameterizedTest
    @MethodSource("wrongCapacities")
    void shouldDetectThatCapacityIsWrong(CapacityCase testCase) throws Exception {
        assertThatThrownBy(() -> builder.addLimit(limit -> limit.capacity(testCase.capacity()).refillGreedy(1, VALID_PERIOD)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveCapacity(testCase.capacity()).getMessage());
    }

    @ParameterizedTest
    @MethodSource("wrongInitialTokens")
    void shouldDetectThatInitialTokensIsWrong(InitialTokensCase testCase) throws Exception {
        assertThatThrownBy(() -> Bucket.builder()
            .addLimit(it -> it.capacity(VALID_CAPACITY).refillGreedy(VALID_CAPACITY, VALID_PERIOD)
                .initialTokens(testCase.initialTokens())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveInitialTokens(testCase.initialTokens()).getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidPeriods")
    void shouldCheckThatPeriodIsInvalidPeriodOfBandwidth(PeriodCase testCase) throws Exception {
        assertThatThrownBy(() -> builder.addLimit(limit -> limit.capacity(VALID_CAPACITY).refillGreedy(VALID_CAPACITY, ofMinutes(testCase.period()))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositivePeriod(ofMinutes(testCase.period()).toNanos()).getMessage());
    }

    @Test
    void shouldCheckThatRefillIsNotNull() throws Exception {
        assertThatThrownBy(() -> builder.addLimit(Bandwidth.classic(42, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullBandwidthRefill().getMessage());
    }

    @Test
    void shouldCheckThatBandwidthIsNotNull() throws Exception {
        assertThatThrownBy(() -> builder.addLimit((Bandwidth) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullBandwidth().getMessage());
    }

    @Test
    void shouldCheckThatBandwidthBuilderIsNotNull() throws Exception {
        assertThatThrownBy(() -> builder.addLimit((Function<BandwidthBuilder.BandwidthBuilderCapacityStage, BandwidthBuilder.BandwidthBuilderBuildStage>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullBuilder().getMessage());

        assertThatThrownBy(() -> BucketConfiguration.builder().addLimit((Function<BandwidthBuilder.BandwidthBuilderCapacityStage, BandwidthBuilder.BandwidthBuilderBuildStage>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullBuilder().getMessage());
    }

    @Test
    void shouldCheckThatRefillPeriodIsNotNull() throws Exception {
        assertThatThrownBy(() -> builder.addLimit(Bandwidth.classic(32, Refill.greedy(1, null))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullRefillPeriod().getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidRefillTokens")
    void shouldDetectThatRefillTokensIsInvalid(RefillTokensCase testCase) throws Exception {
        assertThatThrownBy(() -> builder.addLimit(limit -> limit.capacity(32).refillGreedy(testCase.refillTokens(), Duration.ofSeconds(1))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositivePeriodTokens(testCase.refillTokens()).getMessage());
    }

    @Test
    void shouldCheckThatTimeMeterIsNotNull() throws Exception {
        assertThatThrownBy(() -> Bucket.builder().withCustomTimePrecision(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullTimeMeter().getMessage());
    }

    @ParameterizedTest
    @MethodSource("bucketTypes")
    void shouldCheckThatListenerIsNotNullWhenDecoratingBucket(BucketTypeCase testCase) throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(3).refillGreedy(3, ofMinutes(1)))
            .build();

        assertThatThrownBy(() -> testCase.bucketType().createBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS).toListenable(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullListener().getMessage());
    }

    @Test
    void shouldCheckThatLimitedBandwidthListIsNotEmpty() throws Exception {
        LocalBucketBuilder builder = Bucket.builder();

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(restrictionsNotSpecified().getMessage());
    }

    @Test
    void shouldCheckThatTokensToConsumeShouldBePositive() throws Exception {
        Bucket bucket = Bucket.builder().addLimit(
            limit -> limit.capacity(3).refillGreedy(3, ofMinutes(1))
        ).build();

        assertThatThrownBy(() -> bucket.tryConsume(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveTokensToConsume(0).getMessage());

        assertThatThrownBy(() -> bucket.tryConsume(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveTokensToConsume(-1).getMessage());

        assertThatThrownBy(() -> bucket.tryConsumeAsMuchAsPossible(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveTokensToConsume(0).getMessage());

        assertThatThrownBy(() -> bucket.tryConsumeAsMuchAsPossible(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveTokensToConsume(-1).getMessage());

        assertThatThrownBy(() -> bucket.asBlocking().tryConsume(0L, VALID_PERIOD.toNanos(), BlockingStrategy.PARKING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveTokensToConsume(0).getMessage());

        assertThatThrownBy(() -> bucket.asBlocking().tryConsume(-1, VALID_PERIOD.toNanos(), BlockingStrategy.PARKING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveTokensToConsume(-1).getMessage());
    }

    @Test
    void shouldDetectTheHighRateOfRefill() throws Exception {
        assertThatThrownBy(() -> Bucket.builder().addLimit(Bandwidth.simple(2, ofNanos(1))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(tooHighRefillRate(1, 2).getMessage());
    }

    @Test
    void shouldDetectTheNegativeTimeOfFirstRefill() throws Exception {
        Instant timeOfFirstRefill = Instant.ofEpochMilli(-10);

        assertThatThrownBy(() -> Bandwidth.classic(2, Refill.intervallyAligned(2, ofMinutes(2), timeOfFirstRefill, true)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveTimeOfFirstRefill(timeOfFirstRefill).getMessage());
    }

    @Test
    void shouldPreventSpecificationOfInitialTokensIfIntervallyAlignedRefillUsedWithUseAdaptiveInitialTokensTrue() throws Exception {
        assertThatThrownBy(() -> {
            Instant timeOfFirstRefill = Instant.now();
            Bandwidth.classic(2, Refill.intervallyAligned(2, ofMinutes(2), timeOfFirstRefill, true))
                .withInitialTokens(1);
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(intervallyAlignedRefillWithAdaptiveInitialTokensIncompatipleWithManualSpecifiedInitialTokens().getMessage());
    }

    @Test
    void shouldPreventSpecificationOfNanoTimeBasedClockIfIntervallyAlignedRefillUsed() throws Exception {
        Instant timeOfFirstRefill = Instant.now();
        Refill refill = Refill.intervallyAligned(2, ofMinutes(2), timeOfFirstRefill, true);
        Bandwidth bandwidth = Bandwidth.classic(2, refill);

        assertThatThrownBy(() -> Bucket.builder().withNanosecondPrecision().addLimit(bandwidth).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(intervallyAlignedRefillCompatibleOnlyWithWallClock().getMessage());

        assertThatThrownBy(() -> Bucket.builder().addLimit(bandwidth).withNanosecondPrecision().build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(intervallyAlignedRefillCompatibleOnlyWithWallClock().getMessage());
    }

    @ParameterizedTest
    @MethodSource("bucketTypes")
    void shouldDetectThatAllBandwidthHasUniqueId(BucketTypeCase testCase) throws Exception {
        assertThatThrownBy(() -> Bucket.builder()
            .addLimit(Bandwidth.simple(1, Duration.ofSeconds(10)).withId("xyz"))
            .addLimit(Bandwidth.simple(100, Duration.ofSeconds(3600)).withId("xyz"))
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(foundTwoBandwidthsWithSameId(0, 1, "xyz").getMessage());
    }

    @Test
    void shouldCheckThatTimeUnitsToWaitShouldBePositive() throws Exception {
        Bucket bucket = Bucket.builder().addLimit(
            Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
        ).build();

        assertThatThrownBy(() -> bucket.asBlocking().tryConsume(1, 0, BlockingStrategy.PARKING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveNanosToWait(0).getMessage());

        assertThatThrownBy(() -> bucket.asBlocking().tryConsume(1, -1, BlockingStrategy.PARKING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nonPositiveNanosToWait(-1).getMessage());
    }

    @ParameterizedTest
    @MethodSource("nonPositiveTokensToAdd")
    void shouldCheckThatTokensIsNotPositiveToAdd(TokensCase testCase) throws Exception {
        Bucket bucket = Bucket.builder().addLimit(
            Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
        ).build();

        assertThatThrownBy(() -> bucket.addTokens(testCase.tokens()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("nonPositiveTokensToForceAdd")
    void shouldCheckThatTokensIsNotPositiveToForceAdd(TokensCase testCase) throws Exception {
        Bucket bucket = Bucket.builder().addLimit(
            Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
        ).build();

        assertThatThrownBy(() -> bucket.forceAddTokens(testCase.tokens()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCheckThatSchedulerPassedToTryConsumeIsNotNull() throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD))
            .build();
        AsyncBucketProxy asyncBucket = BucketType.GRID.createAsyncBucket(configuration);

        assertThatThrownBy(() -> asyncBucket.asScheduler().tryConsume(32, 1000_000, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullScheduler().getMessage());
    }

    @Test
    void gridBucketShouldCheckThatConfigurationIsNotNull() throws Exception {
        ProxyManagerMock mockProxy = new ProxyManagerMock(TimeMeter.SYSTEM_MILLISECONDS);

        assertThatThrownBy(() -> mockProxy.builder()
            .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
            .build("66", (BucketConfiguration) null))
            .isInstanceOf(Exception.class)
            .hasMessage(nullConfiguration().getMessage());

        assertThatThrownBy(() -> mockProxy.builder()
            .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
            .build("66", () -> null)
            .getAvailableTokens())
            .isInstanceOf(Exception.class)
            .hasMessage(nullConfiguration().getMessage());

        assertThatThrownBy(() -> mockProxy.builder()
            .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
            .build("66", (Supplier<BucketConfiguration>) null))
            .isInstanceOf(Exception.class)
            .hasMessage(nullConfigurationSupplier().getMessage());

        assertThatThrownBy(() -> mockProxy.asAsync().builder()
            .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
            .build("66", (BucketConfiguration) null))
            .isInstanceOf(Exception.class)
            .hasMessage(nullConfiguration().getMessage());

        Throwable nullFutureThrown = catchThrowable(() -> mockProxy.asAsync().builder()
            .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
            .build("66", () -> null)
            .getAvailableTokens().get());
        assertThat(nullFutureThrown).isInstanceOf(Exception.class);
        assertThat(nullFutureThrown.getCause()).hasMessage(nullConfigurationFuture().getMessage());

        Throwable nullCompletedFutureThrown = catchThrowable(() -> mockProxy.asAsync().builder()
            .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
            .build("66", () -> CompletableFuture.completedFuture(null))
            .getAvailableTokens().get());
        assertThat(nullCompletedFutureThrown).isInstanceOf(Exception.class);
        assertThat(nullCompletedFutureThrown.getCause()).hasMessage(nullConfiguration().getMessage());

        assertThatThrownBy(() -> mockProxy.asAsync().builder()
            .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
            .build("66", (Supplier<CompletableFuture<BucketConfiguration>>) null))
            .isInstanceOf(Exception.class)
            .hasMessage(nullConfigurationSupplier().getMessage());
    }

    @ParameterizedTest
    @MethodSource("bucketTypes")
    void shouldDetectThatConfigurationIsNullDuringConfigurationReplacement(BucketTypeCase testCase) throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder().addLimit(Bandwidth.simple(1, Duration.ofSeconds(10))).build();
        Bucket bucket = testCase.bucketType().createBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
        AsyncBucketProxy asyncBucket = testCase.bucketType().createAsyncBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);

        assertThatThrownBy(() -> bucket.replaceConfiguration(null, TokensInheritanceStrategy.AS_IS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullConfiguration().getMessage());

        assertThatThrownBy(() -> bucket.asVerbose().replaceConfiguration(null, TokensInheritanceStrategy.AS_IS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullConfiguration().getMessage());

        assertThatThrownBy(() -> asyncBucket.replaceConfiguration(null, TokensInheritanceStrategy.AS_IS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullConfiguration().getMessage());

        assertThatThrownBy(() -> asyncBucket.asVerbose().replaceConfiguration(null, TokensInheritanceStrategy.AS_IS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullConfiguration().getMessage());
    }

    @ParameterizedTest
    @MethodSource("bucketTypes")
    void shouldDetectThatTokenMigrationModeIsNullDuringConfigurationReplacement(BucketTypeCase testCase) throws Exception {
        ConfigurationBuilder builder = BucketConfiguration.builder().addLimit(Bandwidth.simple(1, Duration.ofSeconds(10)));
        Bucket bucket = testCase.bucketType().createBucket(builder.build(), TimeMeter.SYSTEM_MILLISECONDS);
        AsyncBucketProxy asyncBucket = testCase.bucketType().createAsyncBucket(builder.build(), TimeMeter.SYSTEM_MILLISECONDS);
        BucketConfiguration newConfiguration = builder.build();

        assertThatThrownBy(() -> bucket.replaceConfiguration(newConfiguration, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullTokensInheritanceStrategy().getMessage());

        assertThatThrownBy(() -> bucket.asVerbose().replaceConfiguration(newConfiguration, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullTokensInheritanceStrategy().getMessage());

        assertThatThrownBy(() -> asyncBucket.replaceConfiguration(newConfiguration, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullTokensInheritanceStrategy().getMessage());

        assertThatThrownBy(() -> asyncBucket.asVerbose().replaceConfiguration(newConfiguration, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(nullTokensInheritanceStrategy().getMessage());
    }

}
