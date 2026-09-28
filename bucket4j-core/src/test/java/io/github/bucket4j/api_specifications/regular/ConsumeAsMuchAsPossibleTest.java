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

@DisplayName("Consume As Much As Possible Specification")
class ConsumeAsMuchAsPossibleTest {

    private record ConsumeAsMuchAsPossibleCase(
        int n,
        long requiredResult,
        BucketConfiguration configuration
    ) {

        @Override
        public String toString() {
            return "n=" + n + ", requiredResult=" + requiredResult;
        }
    }

    private record ConsumeAsMuchAsPossibleWithLimitCase(
        int n,
        long requiredResult,
        long limit,
        BucketConfiguration configuration
    ) {

        @Override
        public String toString() {
            return "n=" + n + ", requiredResult=" + requiredResult + ", limit=" + limit;
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

    static Stream<ConsumeAsMuchAsPossibleCase> consumeAsMuchAsPossibleCases() {
        return Stream.of(
            new ConsumeAsMuchAsPossibleCase(1, 0,
                BucketConfiguration.builder().addLimit(it -> it.capacity(10).refillGreedy(10, Duration.ofMinutes(100)).initialTokens(0)).build()),
            new ConsumeAsMuchAsPossibleCase(2, 2,
                BucketConfiguration.builder().addLimit(it -> it.capacity(10).refillGreedy(10, Duration.ofMinutes(100)).initialTokens(2)).build())
        );
    }

    static Stream<ConsumeAsMuchAsPossibleWithLimitCase> consumeAsMuchAsPossibleWithLimitCases() {
        return Stream.of(
            new ConsumeAsMuchAsPossibleWithLimitCase(1, 4, 5,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(4)).build()),
            new ConsumeAsMuchAsPossibleWithLimitCase(2, 5, 5,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5)).build()),
            new ConsumeAsMuchAsPossibleWithLimitCase(3, 5, 5,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5)).build())
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
    @MethodSource("consumeAsMuchAsPossibleCases")
    void shouldReturnRequiredResultWhenConsumeAsMuchAsPossible(ConsumeAsMuchAsPossibleCase testCase) throws Exception {
        for (BucketType bucketType : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0);
            Bucket bucket = bucketType.createBucket(testCase.configuration(), timeMeter);
            assertThat(bucket.tryConsumeAsMuchAsPossible()).isEqualTo(testCase.requiredResult());

            AsyncBucketProxy asyncBucket = bucketType.createAsyncBucket(testCase.configuration(), timeMeter);
            assertThat(asyncBucket.tryConsumeAsMuchAsPossible().get()).isEqualTo(testCase.requiredResult());
        }
    }

    @ParameterizedTest
    @MethodSource("consumeAsMuchAsPossibleWithLimitCases")
    void shouldReturnRequiredResultWhenTryingToConsumeAsMuchAsPossibleWithLimit(ConsumeAsMuchAsPossibleWithLimitCase testCase) throws Exception {
        for (BucketType bucketType : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0);
            Bucket bucket = bucketType.createBucket(testCase.configuration(), timeMeter);
            assertThat(bucket.tryConsumeAsMuchAsPossible(testCase.limit())).isEqualTo(testCase.requiredResult());

            AsyncBucketProxy asyncBucket = bucketType.createAsyncBucket(testCase.configuration(), timeMeter);
            assertThat(asyncBucket.tryConsumeAsMuchAsPossible(testCase.limit()).get()).isEqualTo(testCase.requiredResult());
        }
    }

    @ParameterizedTest
    @MethodSource("typeAndVerbose")
    void testListenerForTryConsumeAsMuchAsPossible(TypeAndVerboseCase testCase) throws Exception {
        Bucket bucket = testCase.type().createBucket(configuration, clock, listener);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible();
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible();
        }
        assertThat(listener.getConsumed()).isEqualTo(10);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible();
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible();
        }
        assertThat(listener.getConsumed()).isEqualTo(10);
        assertThat(listener.getRejected()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("typeAndVerbose")
    void testListenerForTryConsumeAsMuchAsPossibleWithLimit(TypeAndVerboseCase testCase) throws Exception {
        Bucket bucket = testCase.type().createBucket(configuration, clock, listener);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible(8);
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible(8);
        }
        assertThat(listener.getConsumed()).isEqualTo(8);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible(8);
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible(8);
        }
        assertThat(listener.getConsumed()).isEqualTo(10);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible(3);
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible(3);
        }
        assertThat(listener.getConsumed()).isEqualTo(10);
        assertThat(listener.getRejected()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("typeAndVerbose")
    void testListenerForAsyncTryConsumeAsMuchAsPossible(TypeAndVerboseCase testCase) throws Exception {
        AsyncBucketProxy bucket = testCase.type().createAsyncBucket(configuration, clock, listener);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible().get();
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible().get();
        }
        assertThat(listener.getConsumed()).isEqualTo(10);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible().get();
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible().get();
        }
        assertThat(listener.getConsumed()).isEqualTo(10);
        assertThat(listener.getRejected()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("typeAndVerbose")
    void testListenerForAsyncTryConsumeAsMuchAsPossibleWithLimit(TypeAndVerboseCase testCase) throws Exception {
        AsyncBucketProxy bucket = testCase.type().createAsyncBucket(configuration, clock, listener);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible(8).get();
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible(8).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(8);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible(8).get();
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible(8).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(10);
        assertThat(listener.getRejected()).isEqualTo(0);

        if (!testCase.verbose()) {
            bucket.tryConsumeAsMuchAsPossible(3).get();
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible(3).get();
        }
        assertThat(listener.getConsumed()).isEqualTo(10);
        assertThat(listener.getRejected()).isEqualTo(0);
    }

}
