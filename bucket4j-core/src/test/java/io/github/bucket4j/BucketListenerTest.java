package io.github.bucket4j;

import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BucketListener Specification")
class BucketListenerTest {

    private record BucketTypeCase(BucketType type) {

        @Override
        public String toString() {
            return "type=" + type;
        }
    }

    static Stream<BucketTypeCase> bucketTypes() {
        return Stream.of(BucketType.values()).map(BucketTypeCase::new);
    }

    @ParameterizedTest
    @MethodSource("bucketTypes")
    void bucketCreatedByToListenableShouldShareTokensWithSourceBucket(BucketTypeCase testCase) throws Exception {
        TimeMeterMock clock = new TimeMeterMock();
        SimpleBucketListener listener = new SimpleBucketListener();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(it -> it.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        Bucket sourceBucket = testCase.type().createBucket(configuration, clock);
        Bucket listenableBucket = sourceBucket.toListenable(listener);

        sourceBucket.tryConsume(9);
        assertThat(sourceBucket.getAvailableTokens()).isEqualTo(1);
        assertThat(listenableBucket.getAvailableTokens()).isEqualTo(1);

        listenableBucket.tryConsume(1);
        assertThat(sourceBucket.getAvailableTokens()).isEqualTo(0);
        assertThat(listenableBucket.getAvailableTokens()).isEqualTo(0);

        assertThat(sourceBucket.tryConsume(1)).isFalse();
        assertThat(listenableBucket.tryConsume(1)).isFalse();
    }

}
