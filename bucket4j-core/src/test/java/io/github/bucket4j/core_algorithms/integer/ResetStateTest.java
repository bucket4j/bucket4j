package io.github.bucket4j.core_algorithms.integer;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.BucketState64BitsInteger;
import io.github.bucket4j.MathType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BucketState#reset(long)} skips the token recalculation that {@link BucketState#refillAllBandwidth(long)}
 * would do, because tokens are restored up to capacity anyway, but it must leave exactly the same refill timestamps
 * behind, otherwise the elapsed time would be accounted twice by the next refill.
 *
 * <p>
 * Each case below compares {@code reset(currentTimeNanos)} against the {@code refillAllBandwidth + restore up to capacity}
 * pair that was used before the optimization, by content of the whole state, which includes the refill timestamps.
 */
@DisplayName("BucketState reset Specification")
class ResetStateTest {

    private record ResetCase(String description, BucketConfiguration configuration,
                             long initTimeNanos, long tokensToConsume, long resetTimeNanos) {

        @Override
        public String toString() {
            return description;
        }
    }

    private static BucketConfiguration greedy() {
        return BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)))
            .build();
    }

    private static BucketConfiguration intervally() {
        return BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(100).refillIntervally(100, Duration.ofNanos(100)))
            .build();
    }

    private static BucketConfiguration greedyAndIntervally() {
        return BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)))
            .addLimit(limit -> limit.capacity(1000).refillIntervally(1000, Duration.ofNanos(7000)))
            .build();
    }

    static Stream<ResetCase> resetCases() {
        return Stream.of(
            new ResetCase("greedy, clock did not move", greedy(), 0, 40, 0),
            new ResetCase("greedy, clock moved inside the period", greedy(), 0, 40, 30),
            new ResetCase("greedy, clock moved over several periods", greedy(), 0, 40, 1234),
            new ResetCase("intervally, clock moved inside the period", intervally(), 0, 40, 30),
            new ResetCase("intervally, clock moved to the period boundary", intervally(), 0, 40, 100),
            new ResetCase("intervally, clock moved over an incomplete period", intervally(), 0, 40, 150),
            new ResetCase("several bandwidths", greedyAndIntervally(), 0, 40, 9500),
            new ResetCase("nothing was consumed", greedy(), 0, 0, 30),
            new ResetCase("everything was consumed", greedy(), 0, 100, 30),

            // TimeMeter does not require timestamps to be non-negative, in particular the default SYSTEM_NANOTIME
            // exposes System#nanoTime as is, so -1 must be treated as a regular timestamp and not as a marker
            new ResetCase("negative clock, greedy, reset exactly at -1", greedy(), -5_000, 40, -1),
            new ResetCase("negative clock, intervally, reset exactly at -1", intervally(), -5_000, 40, -1),
            new ResetCase("negative clock, greedy, reset before zero", greedy(), -5_000, 40, -2_500),
            new ResetCase("negative clock, greedy, reset after zero", greedy(), -5_000, 40, 2_500),
            new ResetCase("negative clock did not move", greedy(), -5_000, 40, -5_000),
            new ResetCase("negative clock moved backward", greedy(), -5_000, 40, -6_000)
        );
    }

    @ParameterizedTest
    @MethodSource("resetCases")
    void resetShouldLeaveTheSameStateAsRefillFollowedByRestoringTokensUpToCapacity(ResetCase testCase) {
        BucketState64BitsInteger expected = createState(testCase);
        expected.refillAllBandwidth(testCase.resetTimeNanos());
        // addTokens with such an argument always restores tokens up to capacity, exactly like reset does,
        // but unlike reset it does not touch the refill timestamps
        expected.addTokens(Long.MAX_VALUE);

        BucketState64BitsInteger actual = createState(testCase);
        actual.reset(testCase.resetTimeNanos());

        assertThat(actual.equalsByContent(expected))
            .withFailMessage("expected %s but was %s", expected, actual)
            .isTrue();
    }

    private static BucketState64BitsInteger createState(ResetCase testCase) {
        BucketState state = BucketState.createInitialState(testCase.configuration(), MathType.INTEGER_64_BITS, testCase.initTimeNanos());
        state.consume(testCase.tokensToConsume());
        return (BucketState64BitsInteger) state;
    }

}
