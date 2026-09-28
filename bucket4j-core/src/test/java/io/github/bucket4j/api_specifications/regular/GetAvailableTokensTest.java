package io.github.bucket4j.api_specifications.regular;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Get Available Tokens Specification")
class GetAvailableTokensTest {

    private record GetAvailableTokensCase(
        int n,
        long nanosSinceBucketCreation,
        long expectedTokens,
        BucketConfiguration configuration
    ) {

        @Override
        public String toString() {
            return "n=" + n + ", nanosSinceBucketCreation=" + nanosSinceBucketCreation + ", expectedTokens=" + expectedTokens;
        }
    }

    static Stream<GetAvailableTokensCase> getAvailableTokensCases() {
        return Stream.of(
            new GetAvailableTokensCase(1, 49, 50,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1)).build()),
            new GetAvailableTokensCase(2, 50, 50,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new GetAvailableTokensCase(3, 50, 100,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70)).build()),
            new GetAvailableTokensCase(4, 0, 0,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new GetAvailableTokensCase(5, 120, 100,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build())
        );
    }

    @ParameterizedTest
    @MethodSource("getAvailableTokensCases")
    void getAvailableTokensSpecification(GetAvailableTokensCase testCase) throws Exception {
        for (BucketType type : BucketType.values()) {
            for (boolean verbose : List.of(true, false)) {
                TimeMeterMock timeMeter = new TimeMeterMock(0);
                Bucket bucket = type.createBucket(testCase.configuration(), timeMeter);
                bucket.estimateAbilityToConsume(1); // touch the bucket in order to create it

                timeMeter.addTime(testCase.nanosSinceBucketCreation());
                if (!verbose) {
                    assertThat(bucket.getAvailableTokens()).isEqualTo(testCase.expectedTokens());
                } else {
                    assertThat(bucket.asVerbose().getAvailableTokens().getValue()).isEqualTo(testCase.expectedTokens());
                }
            }
        }
    }

}
