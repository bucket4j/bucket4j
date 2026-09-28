package io.github.bucket4j.api_specifications.regular;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reset Tokens Specification")
class ResetTokensTest {

    @Test
    void resetBucketSpec() throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(it -> it.capacity(100).refillGreedy(100, Duration.ofNanos(100)).initialTokens(10))
            .build();
        for (BucketType type : BucketType.values()) {
            for (boolean sync : List.of(true, false)) {
                for (boolean verbose : List.of(true, false)) {
                    System.out.println("type=" + type + " sync=" + sync + " verbose=" + verbose);
                    TimeMeterMock timeMeter = new TimeMeterMock(0);
                    if (sync) {
                        Bucket bucket = type.createBucket(configuration, timeMeter);
                        bucket.getAvailableTokens();
                        if (!verbose) {
                            bucket.reset();
                        } else {
                            bucket.asVerbose().reset();
                        }
                        assertThat(bucket.getAvailableTokens()).isEqualTo(100);
                    } else {
                        AsyncBucketProxy bucket = type.createAsyncBucket(configuration, timeMeter);
                        bucket.getAvailableTokens();
                        if (!verbose) {
                            bucket.reset().get();
                        } else {
                            bucket.asVerbose().reset().get();
                        }
                        assertThat(bucket.getAvailableTokens().get()).isEqualTo(100);
                    }
                }
            }
        }
    }

}
