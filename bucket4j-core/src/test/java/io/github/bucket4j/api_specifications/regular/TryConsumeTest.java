package io.github.bucket4j.api_specifications.regular;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
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

@DisplayName("Try Consume Specification")
class TryConsumeTest {

    private record TryConsumeCase(
        int n,
        boolean requiredResult,
        long toConsume,
        BucketConfiguration configuration
    ) {

        @Override
        public String toString() {
            return "n=" + n + ", requiredResult=" + requiredResult + ", toConsume=" + toConsume;
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

    static Stream<TryConsumeCase> tryConsumeCases() {
        return Stream.of(
            new TryConsumeCase(1, false, 1,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0)).build()),
            new TryConsumeCase(2, true, 1,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(1)).build())
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
    @MethodSource("tryConsumeCases")
    void shouldReturnRequiredResultWhenTryingToConsume(TryConsumeCase testCase) throws Exception {
        for (BucketType type : BucketType.values()) {
            System.out.println(type);
            TimeMeterMock timeMeter = new TimeMeterMock(0);
            Bucket bucket = type.createBucket(testCase.configuration(), timeMeter);
            assertThat(bucket.tryConsume(testCase.toConsume())).isEqualTo(testCase.requiredResult());

            AsyncBucketProxy asyncBucket = type.createAsyncBucket(testCase.configuration(), timeMeter);
            assertThat(asyncBucket.tryConsume(testCase.toConsume()).get()).isEqualTo(testCase.requiredResult());
        }
    }

    @ParameterizedTest
    @MethodSource("typeAndVerbose")
    void testListenerForTryConsume(TypeAndVerboseCase testCase) throws Exception {
        Bucket bucket = testCase.type().createBucket(configuration, clock, listener);

        boolean consumed;
        if (!testCase.verbose()) {
            consumed = bucket.tryConsume(9);
        } else {
            consumed = bucket.asVerbose().tryConsume(9).getValue();
        }
        assertThat(consumed).isTrue();
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsume(6);
        } else {
            bucket.asVerbose().tryConsume(6);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(6);
    }

    @ParameterizedTest
    @MethodSource("typeAndVerbose")
    void testListenerForAsyncTryConsume(TypeAndVerboseCase testCase) throws Exception {
        AsyncBucketProxy bucket = testCase.type().createAsyncBucket(configuration, clock, listener);

        if (!testCase.verbose()) {
            bucket.tryConsume(9).get();
        } else {
            bucket.asVerbose().tryConsume(9).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsume(6).get();
        } else {
            bucket.asVerbose().tryConsume(6).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(6);
    }

}
