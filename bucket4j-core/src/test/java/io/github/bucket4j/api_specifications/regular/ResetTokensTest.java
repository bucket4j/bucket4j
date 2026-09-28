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

    @Test
    @DisplayName("reset should move greedy refill timestamp to the moment of reset")
    void resetShouldSyncGreedyRefillTimestampSpec() throws Exception {
        // 1 token per nanosecond
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(it -> it.capacity(100).refillGreedy(100, Duration.ofNanos(100)))
            .build();
        for (BucketType type : BucketType.values()) {
            for (boolean sync : List.of(true, false)) {
                for (boolean verbose : List.of(true, false)) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0);
                    if (sync) {
                        Bucket bucket = type.createBucket(configuration, timeMeter);
                        assertThat(bucket.tryConsume(100)).isTrue();
                        timeMeter.addTime(30);

                        if (!verbose) {
                            bucket.reset();
                        } else {
                            bucket.asVerbose().reset();
                        }
                        assertThat(bucket.tryConsume(100)).isTrue();
                        assertThat(bucket.getAvailableTokens()).isZero();

                        timeMeter.addTime(10);
                        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
                    } else {
                        AsyncBucketProxy bucket = type.createAsyncBucket(configuration, timeMeter);
                        assertThat(bucket.tryConsume(100).get()).isTrue();
                        timeMeter.addTime(30);

                        if (!verbose) {
                            bucket.reset().get();
                        } else {
                            bucket.asVerbose().reset().get();
                        }
                        assertThat(bucket.tryConsume(100).get()).isTrue();
                        assertThat(bucket.getAvailableTokens().get()).isZero();

                        timeMeter.addTime(10);
                        assertThat(bucket.getAvailableTokens().get()).isEqualTo(10);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("reset should keep the refill boundary of intervally refilled bandwidth")
    void resetShouldKeepIntervalRefillBoundarySpec() throws Exception {
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(it -> it.capacity(100).refillIntervally(100, Duration.ofNanos(100)))
            .build();
        for (BucketType type : BucketType.values()) {
            for (boolean sync : List.of(true, false)) {
                for (boolean verbose : List.of(true, false)) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0);
                    if (sync) {
                        Bucket bucket = type.createBucket(configuration, timeMeter);
                        assertThat(bucket.tryConsume(100)).isTrue();
                        timeMeter.addTime(150);

                        if (!verbose) {
                            bucket.reset();
                        } else {
                            bucket.asVerbose().reset();
                        }
                        assertThat(bucket.tryConsume(100)).isTrue();

                        // the refill boundary stays aligned to the period grid that started at bucket creation
                        timeMeter.addTime(49);
                        assertThat(bucket.getAvailableTokens()).isZero();
                        timeMeter.addTime(1);
                        assertThat(bucket.getAvailableTokens()).isEqualTo(100);
                    } else {
                        AsyncBucketProxy bucket = type.createAsyncBucket(configuration, timeMeter);
                        assertThat(bucket.tryConsume(100).get()).isTrue();
                        timeMeter.addTime(150);

                        if (!verbose) {
                            bucket.reset().get();
                        } else {
                            bucket.asVerbose().reset().get();
                        }
                        assertThat(bucket.tryConsume(100).get()).isTrue();

                        timeMeter.addTime(49);
                        assertThat(bucket.getAvailableTokens().get()).isZero();
                        timeMeter.addTime(1);
                        assertThat(bucket.getAvailableTokens().get()).isEqualTo(100);
                    }
                }
            }
        }
    }

}
