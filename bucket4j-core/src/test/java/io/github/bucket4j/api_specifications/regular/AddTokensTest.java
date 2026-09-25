package io.github.bucket4j.api_specifications.regular;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Add Tokens Specification")
class AddTokensTest {

    private record AddTokensCase(
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

    static Stream<AddTokensCase> addTokensSpecCases() {
        return Stream.of(
            new AddTokensCase(1, 49, 50, 99,
                BucketConfiguration.builder().addLimit(it -> it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()),
            new AddTokensCase(2, 50, 50, 100,
                BucketConfiguration.builder().addLimit(it -> it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()),
            new AddTokensCase(3, 50, 0, 50,
                BucketConfiguration.builder().addLimit(it -> it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()),
            new AddTokensCase(4, 120, 0, 100,
                BucketConfiguration.builder().addLimit(it -> it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build()),
            new AddTokensCase(5, 120, 110, 100,
                BucketConfiguration.builder().addLimit(it -> it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(0)).build())
        );
    }

    @ParameterizedTest
    @MethodSource("addTokensSpecCases")
    void addTokensSpec(AddTokensCase testCase) throws Exception {
        for (BucketType type : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0);
            Bucket bucket = type.createBucket(testCase.configuration(), timeMeter);
            bucket.getAvailableTokens(); // touch the bucket in order to initialize

            timeMeter.addTime(testCase.nanosIncrement());
            bucket.addTokens(testCase.tokensToAdd());
            assertThat(bucket.getAvailableTokens()).isEqualTo(testCase.requiredResult());

            timeMeter = new TimeMeterMock(0);
            AsyncBucketProxy asyncBucket = type.createAsyncBucket(testCase.configuration(), timeMeter);
            asyncBucket.getAvailableTokens().get(); // touch the bucket in order to initialize
            timeMeter.addTime(testCase.nanosIncrement());
            asyncBucket.addTokens(testCase.tokensToAdd()).get();
            assertThat(asyncBucket.getAvailableTokens().get()).isEqualTo(testCase.requiredResult());
        }
    }

}
