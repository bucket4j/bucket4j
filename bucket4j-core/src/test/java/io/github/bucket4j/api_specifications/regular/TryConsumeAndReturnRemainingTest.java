package io.github.bucket4j.api_specifications.regular;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.SimpleBucketListener;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Try Consume And Return Remaining Specification")
class TryConsumeAndReturnRemainingTest {

    private record TryConsumeAndReturnRemainingCase(
        int n,
        long toConsume,
        boolean result,
        long expectedRemaining,
        long expectedWait,
        BucketConfiguration configuration
    ) {

        @Override
        public String toString() {
            return "n=" + n + ", toConsume=" + toConsume + ", result=" + result
                + ", expectedRemaining=" + expectedRemaining + ", expectedWait=" + expectedWait;
        }
    }

    private record TypeAndVerboseCase(BucketType type, boolean verbose) {

        @Override
        public String toString() {
            return "type=" + type + ", verbose=" + verbose;
        }
    }

    private final TimeMeterMock clock = new TimeMeterMock();
    private final SimpleBucketListener listener = new SimpleBucketListener();

    private final BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();

    static Stream<TryConsumeAndReturnRemainingCase> tryConsumeAndReturnRemainingCases() {
        return Stream.of(
            new TryConsumeAndReturnRemainingCase(1, 49, true, 51, 0,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(100)).build()),
            new TryConsumeAndReturnRemainingCase(2, 1, true, 0, 0,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1)).build()),
            new TryConsumeAndReturnRemainingCase(3, 80, false, 70, 10,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70)).build()),
            new TryConsumeAndReturnRemainingCase(4, 10, false, 0, 10,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new TryConsumeAndReturnRemainingCase(5, 120, false, 10, Long.MAX_VALUE,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(10)).build())
        );
    }

    static Stream<TypeAndVerboseCase> typeAndVerbose() {
        List<TypeAndVerboseCase> cases = new ArrayList<>();
        for (BucketType type : BucketType.values()) {
            for (boolean verbose : List.of(false, true)) {
                cases.add(new TypeAndVerboseCase(type, verbose));
            }
        }
        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("tryConsumeAndReturnRemainingCases")
    void tryConsumeAndReturnRemainingSpecification(TryConsumeAndReturnRemainingCase testCase) throws Exception {
        for (BucketType type : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0);
            Bucket bucket = type.createBucket(testCase.configuration(), timeMeter);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(testCase.toConsume());
            assertThat(probe.isConsumed()).isEqualTo(testCase.result());
            assertThat(probe.getRemainingTokens()).isEqualTo(testCase.expectedRemaining());
            assertThat(probe.getNanosToWaitForRefill()).isEqualTo(testCase.expectedWait());

            AsyncBucketProxy asyncBucket = type.createAsyncBucket(testCase.configuration(), timeMeter);
            probe = asyncBucket.tryConsumeAndReturnRemaining(testCase.toConsume()).get();
            assertThat(probe.isConsumed()).isEqualTo(testCase.result());
            assertThat(probe.getRemainingTokens()).isEqualTo(testCase.expectedRemaining());
            assertThat(probe.getNanosToWaitForRefill()).isEqualTo(testCase.expectedWait());
        }
    }

    @ParameterizedTest
    @MethodSource("typeAndVerbose")
    void testListenerForTryConsumeAndReturnRemaining(TypeAndVerboseCase testCase) throws Exception {
        Bucket bucket = testCase.type().createBucket(configuration, clock, listener);

        if (!testCase.verbose()) {
            bucket.tryConsumeAndReturnRemaining(9);
        } else {
            bucket.asVerbose().tryConsumeAndReturnRemaining(9);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsumeAndReturnRemaining(6);
        } else {
            bucket.asVerbose().tryConsumeAndReturnRemaining(6);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(6);
    }

    @ParameterizedTest
    @MethodSource("typeAndVerbose")
    void testListenerForAsyncTryConsumeAndReturnRemaining(TypeAndVerboseCase testCase) throws Exception {
        AsyncBucketProxy bucket = testCase.type().createAsyncBucket(configuration, clock, listener);

        if (!testCase.verbose()) {
            bucket.tryConsumeAndReturnRemaining(9).get();
        } else {
            bucket.asVerbose().tryConsumeAndReturnRemaining(9).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsumeAndReturnRemaining(6).get();
        } else {
            bucket.asVerbose().tryConsumeAndReturnRemaining(6).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(6);
    }

}
