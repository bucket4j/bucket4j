package io.github.bucket4j.core_algorithms.integer;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.MathType;
import io.github.bucket4j.Refill;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BucketState Specification")
class BucketStateTest {

    private static final TimeMeter TIME_METER = new TimeMeterMock();

    private record GetAvailableTokensCase(String testNumber, long requiredAvailableTokens, Bucket bucket) {

        @Override
        public String toString() {
            return testNumber;
        }
    }

    private record AddTokensCase(String testNumber, long tokensToAdd, long requiredAvailableTokens, Bucket bucket) {

        @Override
        public String toString() {
            return testNumber;
        }
    }

    private record DelayAfterWillBePossibleToConsumeCase(String testNumber, long toConsume, long requiredTime, Bucket bucket) {

        @Override
        public String toString() {
            return testNumber;
        }
    }

    private record CalculateFullRefillingTimeCase(String testNumber, long requiredTime,
                                                  long timeShiftBeforeAsk, long tokensConsumeBeforeAsk, BucketConfiguration configuration) {

        @Override
        public String toString() {
            return testNumber;
        }
    }

    private record RefillSimpleBandwidthCase(int n, long initialTokens, long capacity, long period,
                                             long initTime, long timeOnRefill, long tokensAfterRefill, long roundingError) {

        @Override
        public String toString() {
            return "#" + n;
        }
    }

    private record RefillClassicBandwidthCase(int n, long initialTokens, long capacity, long refillTokens, long refillPeriod,
                                              long initTime, long timeOnRefill, long tokensAfterRefill, long roundingError) {

        @Override
        public String toString() {
            return "#" + n;
        }
    }

    private record ConsumeCase(int n, long initialTokens, long period, long capacity, long toConsume, long requiredSize) {

        @Override
        public String toString() {
            return "#" + n;
        }
    }

    static Stream<GetAvailableTokensCase> getAvailableTokensSpecification() {
        return Stream.of(
            new GetAvailableTokensCase("#1", 10,
                Bucket.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)))
                    .withCustomTimePrecision(TIME_METER)
                    .build()),
            new GetAvailableTokensCase("#2", 0,
                Bucket.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
                    .withCustomTimePrecision(TIME_METER)
                    .build()),
            new GetAvailableTokensCase("#3", 5,
                Bucket.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(5))
                    .withCustomTimePrecision(TIME_METER)
                    .build()),
            new GetAvailableTokensCase("#4", 2,
                Bucket.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(5))
                    .addLimit(Bandwidth.simple(2, Duration.ofNanos(100)))
                    .withCustomTimePrecision(TIME_METER)
                    .build()),
            new GetAvailableTokensCase("#5", 10,
                Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(1, Duration.ofSeconds(1))))
                    .build())
        );
    }

    static Stream<AddTokensCase> addTokensSpecification() {
        return Stream.of(
            new AddTokensCase("#1", 10, 10,
                Bucket.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
                    .withCustomTimePrecision(TIME_METER)
                    .build()),
            new AddTokensCase("#2", 1, 10,
                Bucket.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)))
                    .withCustomTimePrecision(TIME_METER)
                    .build()),
            new AddTokensCase("#3", 6, 10,
                Bucket.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(5))
                    .withCustomTimePrecision(TIME_METER)
                    .build()),
            new AddTokensCase("#4", 3, 2,
                Bucket.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(5))
                    .addLimit(Bandwidth.simple(2, Duration.ofNanos(100)))
                    .withCustomTimePrecision(TIME_METER)
                    .build()),
            new AddTokensCase("#5", 4, 5,
                Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(1, Duration.ofSeconds(1))).withInitialTokens(1))
                    .withCustomTimePrecision(TIME_METER)
                    .build())
        );
    }

    static Stream<DelayAfterWillBePossibleToConsumeCase> delayAfterWillBePossibleToConsumeSpecification() {
        return Stream.of(
            new DelayAfterWillBePossibleToConsumeCase("#1", 10, 100,
                Bucket.builder()
                    .withCustomTimePrecision(new TimeMeterMock(0))
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
                    .build()),
            new DelayAfterWillBePossibleToConsumeCase("#2", 10, 100,
                Bucket.builder()
                    .withCustomTimePrecision(new TimeMeterMock(0))
                    .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofNanos(100))).withInitialTokens(0))
                    .build()),
            new DelayAfterWillBePossibleToConsumeCase("#3", 10, 500,
                Bucket.builder()
                    .withCustomTimePrecision(new TimeMeterMock(0))
                    .addLimit(Bandwidth.classic(10, Refill.greedy(2, Duration.ofNanos(100))).withInitialTokens(0))
                    .build()),
            new DelayAfterWillBePossibleToConsumeCase("#4", 7, 30,
                Bucket.builder()
                    .withCustomTimePrecision(new TimeMeterMock(0))
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(4))
                    .build()),
            new DelayAfterWillBePossibleToConsumeCase("#5", 11, 70,
                Bucket.builder()
                    .withCustomTimePrecision(new TimeMeterMock(0))
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(4))
                    .build()),
            new DelayAfterWillBePossibleToConsumeCase("#6", 3, 20,
                Bucket.builder()
                    .withCustomTimePrecision(new TimeMeterMock(0))
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
                    .addLimit(Bandwidth.simple(5, Duration.ofNanos(10)).withInitialTokens(2))
                    .build()),
            new DelayAfterWillBePossibleToConsumeCase("#7", 3, 20,
                Bucket.builder()
                    .withCustomTimePrecision(new TimeMeterMock(0))
                    .addLimit(Bandwidth.simple(5, Duration.ofNanos(10)).withInitialTokens(2))
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
                    .build()),
            new DelayAfterWillBePossibleToConsumeCase("#8", 3, 0,
                Bucket.builder()
                    .withCustomTimePrecision(new TimeMeterMock(0))
                    .addLimit(Bandwidth.simple(5, Duration.ofNanos(10)).withInitialTokens(5))
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(3))
                    .build())
        );
    }

    static Stream<CalculateFullRefillingTimeCase> calculateFullRefillingTimeSpecification() {
        return Stream.of(
            new CalculateFullRefillingTimeCase("#1", 90, 0, 0,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
                    .build()),
            new CalculateFullRefillingTimeCase("#2", 100, 0, 0,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofNanos(100))).withInitialTokens(1))
                    .build()),
            new CalculateFullRefillingTimeCase("#3", 1650, 0, 23,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(2, Duration.ofNanos(100))).withInitialTokens(0))
                    .build()),
            new CalculateFullRefillingTimeCase("#4", 1700, 0, 23,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(10, Refill.intervally(2, Duration.ofNanos(100))).withInitialTokens(0))
                    .build()),
            new CalculateFullRefillingTimeCase("#5", 60, 0, 0,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(4))
                    .build()),
            new CalculateFullRefillingTimeCase("#6", 90, 0, 0,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
                    .addLimit(Bandwidth.simple(5, Duration.ofNanos(10)).withInitialTokens(2))
                    .build()),
            new CalculateFullRefillingTimeCase("#7", 90, 0, 0,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(5, Duration.ofNanos(10)).withInitialTokens(2))
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
                    .build()),
            new CalculateFullRefillingTimeCase("#8", 70, 0, 0,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(5, Duration.ofNanos(10)).withInitialTokens(5))
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(3))
                    .build())
        );
    }

    static Stream<RefillSimpleBandwidthCase> refillSimpleBandwidthSpecification() {
        return Stream.of(
            new RefillSimpleBandwidthCase(1, 0, 1000, 1000, 10000, 10040, 40, 0),
            new RefillSimpleBandwidthCase(2, 50, 1000, 1000, 10000, 10001, 51, 0),
            new RefillSimpleBandwidthCase(3, 55, 1000, 1000, 10000, 9999, 55, 0),
            new RefillSimpleBandwidthCase(4, 200, 1000, 1000, 10000, 20000, 1000, 0),
            new RefillSimpleBandwidthCase(5, 0, 100, 1000, 10000, 10003, 0, 300),
            new RefillSimpleBandwidthCase(6, 90, 100, 1000, 10000, 10017, 91, 700),
            new RefillSimpleBandwidthCase(7, 0, 100, 1000, 10000, 28888, 100, 0)
        );
    }

    static Stream<RefillClassicBandwidthCase> refillClassicBandwidthSpecification() {
        return Stream.of(
            new RefillClassicBandwidthCase(1, 0, 1000, 1, 1, 10000, 10040, 40, 0),
            new RefillClassicBandwidthCase(2, 50, 1000, 10, 10, 10000, 10001, 51, 0),
            new RefillClassicBandwidthCase(3, 55, 1000, 1, 1, 10000, 10000, 55, 0),
            new RefillClassicBandwidthCase(4, 200, 1000, 10, 10, 10000, 20000, 1000, 0),
            new RefillClassicBandwidthCase(5, 0, 100, 1, 10, 10000, 10003, 0, 3),
            new RefillClassicBandwidthCase(6, 90, 100, 1, 10, 10000, 10017, 91, 7),
            new RefillClassicBandwidthCase(7, 0, 100, 1, 10, 10000, 28888, 100, 0)
        );
    }

    static Stream<ConsumeCase> consumeSpecification() {
        return Stream.of(
            new ConsumeCase(1, 0, 1000, 1000, 10, -10),
            new ConsumeCase(2, 50, 1000, 1000, 2, 48),
            new ConsumeCase(3, 55, 1000, 1000, 1600, -1545)
        );
    }

    @ParameterizedTest
    @MethodSource("getAvailableTokensSpecification")
    void getAvailableTokensSpecification(GetAvailableTokensCase testCase) throws Exception {
        BucketState state = testCase.bucket().asVerbose().getAvailableTokens().getState();

        long availableTokens = state.getAvailableTokens();
        assertThat(availableTokens).isEqualTo(testCase.requiredAvailableTokens());
    }

    @ParameterizedTest
    @MethodSource("addTokensSpecification")
    void addTokensSpecification(AddTokensCase testCase) throws Exception {
        BucketState state = testCase.bucket().asVerbose().getAvailableTokens().getState();

        state.addTokens(testCase.tokensToAdd());
        long availableTokens = state.getAvailableTokens();
        assertThat(availableTokens).isEqualTo(testCase.requiredAvailableTokens());
    }

    @ParameterizedTest
    @MethodSource("delayAfterWillBePossibleToConsumeSpecification")
    void delayAfterWillBePossibleToConsumeSpecification(DelayAfterWillBePossibleToConsumeCase testCase) throws Exception {
        TimeMeter timeMeter = ((LocalBucket) testCase.bucket()).getTimeMeter();
        BucketState state = testCase.bucket().asVerbose().getAvailableTokens().getState();

        long actualTime = state.calculateDelayNanosAfterWillBePossibleToConsume(testCase.toConsume(), timeMeter.currentTimeNanos(), false);
        assertThat(actualTime).isEqualTo(testCase.requiredTime());
    }

    @ParameterizedTest
    @MethodSource("calculateFullRefillingTimeSpecification")
    void calculateFullRefillingTimeSpecification(CalculateFullRefillingTimeCase testCase) throws Exception {
        BucketState state = BucketState.createInitialState(testCase.configuration(), MathType.INTEGER_64_BITS, 0L);
        state.refillAllBandwidth(testCase.timeShiftBeforeAsk());
        state.consume(testCase.tokensConsumeBeforeAsk());

        long actualTime = state.calculateFullRefillingTime(testCase.timeShiftBeforeAsk());
        assertThat(actualTime).isEqualTo(testCase.requiredTime());
    }

    @ParameterizedTest
    @MethodSource("refillSimpleBandwidthSpecification")
    void refillSimpleBandwidthSpecification(RefillSimpleBandwidthCase testCase) throws Exception {
        TimeMeterMock mockTimer = new TimeMeterMock(testCase.initTime());
        Bucket bucket = Bucket.builder()
            .addLimit(limit -> limit.capacity(testCase.capacity())
                .refillGreedy(testCase.capacity(), Duration.ofNanos(testCase.period()))
                .initialTokens(testCase.initialTokens()))
            .withCustomTimePrecision(mockTimer)
            .build();
        BucketState state = bucket.asVerbose().getAvailableTokens().getState();

        mockTimer.setCurrentTimeNanos(testCase.timeOnRefill());
        state.refillAllBandwidth(testCase.timeOnRefill());

        assertThat(state.getCurrentSize(0)).isEqualTo(testCase.tokensAfterRefill());
        assertThat(state.getRoundingError(0)).isEqualTo(testCase.roundingError());
    }

    @ParameterizedTest
    @MethodSource("refillClassicBandwidthSpecification")
    void refillClassicBandwidthSpecification(RefillClassicBandwidthCase testCase) throws Exception {
        TimeMeterMock mockTimer = new TimeMeterMock(testCase.initTime());
        Refill refill = Refill.greedy(testCase.refillTokens(), Duration.ofNanos(testCase.refillPeriod()));
        Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.classic(testCase.capacity(), refill).withInitialTokens(testCase.initialTokens()))
            .withCustomTimePrecision(mockTimer)
            .build();
        BucketState state = bucket.asVerbose().getAvailableTokens().getState();

        mockTimer.setCurrentTimeNanos(testCase.timeOnRefill());
        state.refillAllBandwidth(testCase.timeOnRefill());

        assertThat(state.getCurrentSize(0)).isEqualTo(testCase.tokensAfterRefill());
        assertThat(state.getRoundingError(0)).isEqualTo(testCase.roundingError());
    }

    @ParameterizedTest
    @MethodSource("consumeSpecification")
    void consumeSpecification(ConsumeCase testCase) throws Exception {
        Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.simple(testCase.capacity(), Duration.ofNanos(testCase.period())).withInitialTokens(testCase.initialTokens()))
            .withCustomTimePrecision(new TimeMeterMock(0))
            .build();
        BucketState state = bucket.asVerbose().getAvailableTokens().getState();

        state.consume(testCase.toConsume());

        assertThat(state.getCurrentSize(0)).isEqualTo(testCase.requiredSize());
    }

}
