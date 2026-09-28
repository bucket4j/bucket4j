package io.github.bucket4j.api_specifications.regular;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Force Add Tokens Specification")
class ForceAddTokensTest {

    private record ForceAddTokensCase(
        int n,
        long tokensToAdd,
        long nanosIncrement,
        long requiredResult,
        BucketConfiguration configuration
    ) {

        @Override
        public String toString() {
            return "n=" + n + ", tokensToAdd=" + tokensToAdd + ", nanosIncrement=" + nanosIncrement;
        }
    }

    static Stream<ForceAddTokensCase> forceAddTokensSpecCases() {
        return Stream.of(
            new ForceAddTokensCase(1, 49, 50, 99,
                BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()),
            new ForceAddTokensCase(2, 50, 50, 100,
                BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()),
            new ForceAddTokensCase(3, 50, 0, 50,
                BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()),
            new ForceAddTokensCase(4, 120, 0, 120,
                BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()),
            new ForceAddTokensCase(5, 120, 110, 220,
                BucketConfiguration.builder().addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build())
        );
    }

    @ParameterizedTest
    @MethodSource("forceAddTokensSpecCases")
    void forceAddTokensSpec(ForceAddTokensCase testCase) throws Exception {
        for (BucketType type : BucketType.values()) {
            for (boolean sync : List.of(true, false)) {
                for (boolean verbose : List.of(true, false)) {
                    System.out.println("type=" + type + " sync=" + sync + " verbose=" + verbose);
                    TimeMeterMock timeMeter = new TimeMeterMock(0);
                    if (sync) {
                        Bucket bucket = type.createBucket(testCase.configuration(), timeMeter);
                        bucket.getAvailableTokens();
                        timeMeter.addTime(testCase.nanosIncrement());
                        if (!verbose) {
                            bucket.forceAddTokens(testCase.tokensToAdd());
                        } else {
                            bucket.asVerbose().forceAddTokens(testCase.tokensToAdd());
                        }
                        assertThat(bucket.getAvailableTokens()).isEqualTo(testCase.requiredResult());
                    } else {
                        AsyncBucketProxy bucket = type.createAsyncBucket(testCase.configuration(), timeMeter);
                        bucket.getAvailableTokens();
                        timeMeter.addTime(testCase.nanosIncrement());
                        if (!verbose) {
                            bucket.forceAddTokens(testCase.tokensToAdd()).get();
                        } else {
                            bucket.asVerbose().forceAddTokens(testCase.tokensToAdd()).get();
                        }
                        assertThat(bucket.getAvailableTokens().get()).isEqualTo(testCase.requiredResult());
                    }
                }
            }
        }
    }

    @Test
    void tokensThatWasAddedOverCapacityShouldNotBeLost() throws Exception {
        TimeMeterMock timeMeter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)))
                .withCustomTimePrecision(timeMeter)
                .build();

        bucket.forceAddTokens(10);
        assertThat(bucket.getAvailableTokens()).isEqualTo(110);

        timeMeter.addTime(10);
        bucket.consumeIgnoringRateLimits(2);
        assertThat(bucket.getAvailableTokens()).isEqualTo(108);

        timeMeter.addTime(10);
        bucket.tryConsume(3);
        assertThat(bucket.getAvailableTokens()).isEqualTo(105);
    }

}
