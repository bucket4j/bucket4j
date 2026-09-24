package io.github.bucket4j.api_specifications.regular;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Estimate Ability To Consume Specification")
class EstimateAbilityToConsumeTest {

    private record EstimateAbilityToConsumeCase(
        int n,
        long toEstimate,
        boolean result,
        long expectedWait,
        BucketConfiguration configuration
    ) {

        @Override
        public String toString() {
            return "n=" + n + ", toEstimate=" + toEstimate + ", result=" + result + ", expectedWait=" + expectedWait;
        }
    }

    static Stream<EstimateAbilityToConsumeCase> estimateAbilityToConsumeCases() {
        return Stream.of(
            new EstimateAbilityToConsumeCase(1, 49, true, 0,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(100)).build()),
            new EstimateAbilityToConsumeCase(2, 1, true, 0,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1)).build()),
            new EstimateAbilityToConsumeCase(3, 80, false, 10,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70)).build()),
            new EstimateAbilityToConsumeCase(4, 10, false, 10,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()),
            new EstimateAbilityToConsumeCase(5, 120, false, Long.MAX_VALUE,
                BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(10)).build()),
            new EstimateAbilityToConsumeCase(6, 80, false, 100,
                BucketConfiguration.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofNanos(100))).withInitialTokens(70)).build()),
            new EstimateAbilityToConsumeCase(7, 10, false, 100,
                BucketConfiguration.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofNanos(100))).withInitialTokens(0)).build()),
            new EstimateAbilityToConsumeCase(8, 120, false, Long.MAX_VALUE,
                BucketConfiguration.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofNanos(100))).withInitialTokens(10)).build())
        );
    }

    @ParameterizedTest
    @MethodSource("estimateAbilityToConsumeCases")
    void estimateAbilityToConsumeSpecification(EstimateAbilityToConsumeCase testCase) throws Exception {
        for (BucketType type : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0);
            Bucket bucket = type.createBucket(testCase.configuration(), timeMeter);
            long availableTokensBeforeEstimation = bucket.getAvailableTokens();
            EstimationProbe probe = bucket.estimateAbilityToConsume(testCase.toEstimate());
            assertThat(probe.canBeConsumed()).isEqualTo(testCase.result());
            assertThat(probe.getRemainingTokens()).isEqualTo(availableTokensBeforeEstimation);
            assertThat(probe.getNanosToWaitForRefill()).isEqualTo(testCase.expectedWait());
            assertThat(bucket.getAvailableTokens()).isEqualTo(availableTokensBeforeEstimation);

            AsyncBucketProxy asyncBucket = type.createAsyncBucket(testCase.configuration(), timeMeter);
            availableTokensBeforeEstimation = bucket.getAvailableTokens();
            probe = asyncBucket.estimateAbilityToConsume(testCase.toEstimate()).get();
            assertThat(probe.canBeConsumed()).isEqualTo(testCase.result());
            assertThat(probe.getRemainingTokens()).isEqualTo(availableTokensBeforeEstimation);
            assertThat(probe.getNanosToWaitForRefill()).isEqualTo(testCase.expectedWait());
            assertThat(bucket.getAvailableTokens()).isEqualTo(availableTokensBeforeEstimation);
        }
    }

}
