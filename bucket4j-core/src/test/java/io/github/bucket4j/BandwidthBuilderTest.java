package io.github.bucket4j;

import io.github.bucket4j.util.ComparableByContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BandwidthBuilder Specification")
class BandwidthBuilderTest {

    static final Instant FIRST_REFILL_TIME = ZonedDateTime.now()
        .truncatedTo(ChronoUnit.HOURS)
        .plus(1, ChronoUnit.HOURS)
        .toInstant();

    private record BandwidthCase(int i, Bandwidth oldStyle, Bandwidth newStyle) {

        @Override
        public String toString() {
            return "#" + i;
        }
    }

    static Stream<BandwidthCase> bandwidthCases() {
        return Stream.of(
            new BandwidthCase(1,
                Bandwidth.simple(10, Duration.ofHours(1)),
                Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofHours(1)).build()),
            new BandwidthCase(2,
                Bandwidth.classic(20, Refill.greedy(10, Duration.ofHours(1))),
                Bandwidth.builder().capacity(20).refillGreedy(10, Duration.ofHours(1)).build()),
            new BandwidthCase(3,
                Bandwidth.classic(20, Refill.intervally(10, Duration.ofHours(1))),
                Bandwidth.builder().capacity(20).refillIntervally(10, Duration.ofHours(1)).build()),
            new BandwidthCase(4,
                Bandwidth.classic(20, Refill.intervallyAligned(10, Duration.ofHours(1), FIRST_REFILL_TIME, false)),
                Bandwidth.builder().capacity(20).refillIntervallyAligned(10, Duration.ofHours(1), FIRST_REFILL_TIME).build()),
            new BandwidthCase(5,
                Bandwidth.classic(20, Refill.intervallyAligned(10, Duration.ofHours(1), FIRST_REFILL_TIME, true)),
                Bandwidth.builder().capacity(20).refillIntervallyAlignedWithAdaptiveInitialTokens(10, Duration.ofHours(1), FIRST_REFILL_TIME).build()),
            new BandwidthCase(6,
                Bandwidth.simple(10, Duration.ofHours(1)).withId("x.y.z"),
                Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofHours(1)).id("x.y.z").build()),
            new BandwidthCase(7,
                Bandwidth.simple(10, Duration.ofHours(1)).withInitialTokens(5),
                Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofHours(1)).initialTokens(5).build())
        );
    }

    @ParameterizedTest
    @MethodSource("bandwidthCases")
    void testBandwidthBuilder(BandwidthCase testCase) throws Exception {
        assertThat(ComparableByContent.equals(testCase.oldStyle(), testCase.newStyle())).isTrue();
    }

}
