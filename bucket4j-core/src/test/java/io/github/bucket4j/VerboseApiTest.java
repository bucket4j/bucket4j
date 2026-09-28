package io.github.bucket4j;

import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VerboseApi Specification")
class VerboseApiTest {

    private record BucketTypeCase(BucketType type) {

        @Override
        public String toString() {
            return "type=" + type;
        }
    }

    private record CalculateFullRefillingTimeCase(String testNumber, long requiredTime,
                                                  long timeShiftBeforeAsk, long tokensConsumeBeforeAsk, BucketConfiguration configuration) {

        @Override
        public String toString() {
            return testNumber;
        }
    }

    private record GetAvailableTokensCase(String testNumber, long requiredAvailableTokens, BucketConfiguration configuration) {

        @Override
        public String toString() {
            return testNumber;
        }
    }

    private record GetAvailableTokensPerEachBandwidthCase(String testNumber, List<Long> requiredAvailableTokens, BucketConfiguration configuration) {

        @Override
        public String toString() {
            return testNumber;
        }
    }

    static Stream<BucketTypeCase> bucketTypes() {
        return Stream.of(BucketType.values()).map(BucketTypeCase::new);
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

    static Stream<GetAvailableTokensCase> getAvailableTokensSpecification() {
        return Stream.of(
            new GetAvailableTokensCase("#1", 3,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(3))
                    .build()),
            new GetAvailableTokensCase("#2", 10,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofNanos(100))))
                    .build()),
            new GetAvailableTokensCase("#3", 1,
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(2, Duration.ofNanos(100))).withInitialTokens(1))
                    .addLimit(Bandwidth.classic(100, Refill.greedy(20, Duration.ofNanos(100))))
                    .build())
        );
    }

    static Stream<GetAvailableTokensPerEachBandwidthCase> getAvailableTokensPerEachBandwidthSpecification() {
        return Stream.of(
            new GetAvailableTokensPerEachBandwidthCase("#1", List.of(3L),
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(3))
                    .build()),
            new GetAvailableTokensPerEachBandwidthCase("#2", List.of(10L),
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofNanos(100))))
                    .build()),
            new GetAvailableTokensPerEachBandwidthCase("#3", List.of(1L, 100L),
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(2, Duration.ofNanos(100))).withInitialTokens(1))
                    .addLimit(Bandwidth.classic(100, Refill.greedy(20, Duration.ofNanos(100))))
                    .build()),
            new GetAvailableTokensPerEachBandwidthCase("#3", List.of(100L, 1L),
                BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(100, Refill.greedy(20, Duration.ofNanos(100))))
                    .addLimit(Bandwidth.classic(10, Refill.greedy(2, Duration.ofNanos(100))).withInitialTokens(1))
                    .build())
        );
    }

    @ParameterizedTest
    @MethodSource("bucketTypes")
    void testVerboseInitialization(BucketTypeCase testCase) throws Exception {
        TimeMeterMock clock = new TimeMeterMock();
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(it -> it.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();

        Bucket bucket = testCase.type().createBucket(configuration, clock);

        assertThat(bucket.asVerbose().getAvailableTokens().getValue()).isEqualTo(10);
    }

    @ParameterizedTest
    @MethodSource("calculateFullRefillingTimeSpecification")
    void calculateFullRefillingTimeSpecification(CalculateFullRefillingTimeCase testCase) throws Exception {
        long currentTimeNanos = 0L;
        BucketState state = BucketState.createInitialState(testCase.configuration(), MathType.INTEGER_64_BITS, currentTimeNanos);
        state.refillAllBandwidth(testCase.timeShiftBeforeAsk());
        state.consume(testCase.tokensConsumeBeforeAsk());
        VerboseResult verboseResult = new VerboseResult(testCase.timeShiftBeforeAsk(), 42, state);

        long actualTime = verboseResult.getDiagnostics().calculateFullRefillingTime();
        assertThat(actualTime).isEqualTo(testCase.requiredTime());
    }

    @ParameterizedTest
    @MethodSource("getAvailableTokensSpecification")
    void getAvailableTokens(GetAvailableTokensCase testCase) throws Exception {
        long currentTimeNanos = 0L;
        BucketState state = BucketState.createInitialState(testCase.configuration(), MathType.INTEGER_64_BITS, currentTimeNanos);
        VerboseResult verboseResult = new VerboseResult(currentTimeNanos, 42, state);

        long availableTokens = verboseResult.getDiagnostics().getAvailableTokens();
        assertThat(availableTokens).isEqualTo(testCase.requiredAvailableTokens());
    }

    @ParameterizedTest
    @MethodSource("getAvailableTokensPerEachBandwidthSpecification")
    void getAvailableTokensPerEachBandwidth(GetAvailableTokensPerEachBandwidthCase testCase) throws Exception {
        long currentTimeNanos = 0L;
        BucketState state = BucketState.createInitialState(testCase.configuration(), MathType.INTEGER_64_BITS, currentTimeNanos);
        VerboseResult verboseResult = new VerboseResult(currentTimeNanos, 42, state);

        long[] availableTokens = verboseResult.getDiagnostics().getAvailableTokensPerEachBandwidth();
        assertThat(availableTokens)
            .containsExactly(testCase.requiredAvailableTokens().stream().mapToLong(Long::longValue).toArray());
    }

}
