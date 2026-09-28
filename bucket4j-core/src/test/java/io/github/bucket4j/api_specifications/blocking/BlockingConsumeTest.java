package io.github.bucket4j.api_specifications.blocking;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.SimpleBucketListener;
import io.github.bucket4j.mock.BlockingStrategyMock;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BlockingConsume Specification")
class BlockingConsumeTest {

    private record TypeVerboseCase(BucketType type, boolean verbose) {

        @Override
        public String toString() {
            return type + " verbose=" + verbose;
        }
    }

    private static Stream<TypeVerboseCase> typeVerboseCases() {
        List<TypeVerboseCase> cases = new ArrayList<>();
        for (BucketType type : BucketType.values()) {
            for (boolean verbose : new boolean[]{false, true}) {
                cases.add(new TypeVerboseCase(type, verbose));
            }
        }
        return cases.stream();
    }

    private static class ParkingTrackingListener extends SimpleBucketListener {
        private long beforeParkingNanos;

        @Override
        public void beforeParking(long nanos) {
            beforeParkingNanos += nanos;
        }
    }

    private final TimeMeterMock clock = new TimeMeterMock();
    private final BlockingStrategyMock blocker = new BlockingStrategyMock(clock);

    private final ParkingTrackingListener listener = new ParkingTrackingListener();

    @ParameterizedTest
    @MethodSource("typeVerboseCases")
    void testForBlockingConsume(TypeVerboseCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean verbose = testCase.verbose();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        Bucket bucket = type.createBucket(configuration, clock);

        if (verbose) {
            bucket.asBlocking().asVerbose().consume(9, blocker);
        } else {
            bucket.asBlocking().consume(9, blocker);
        }
        assertThat(blocker.getParkedNanos()).isEqualTo(0);

        if (verbose) {
            bucket.asBlocking().asVerbose().consume(2, blocker);
        } else {
            bucket.asBlocking().consume(2, blocker);
        }
        assertThat(blocker.getParkedNanos()).isEqualTo(100_000_000);

        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> {
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(1, blocker);
            } else {
                bucket.asBlocking().consume(1, blocker);
            }
        }).isInstanceOf(InterruptedException.class);
        assertThat(Thread.interrupted()).isFalse();
        assertThat(blocker.getParkedNanos()).isEqualTo(100_000_000);
        assertThat(blocker.getAtemptToParkNanos()).isEqualTo(200_000_000);

        assertThatThrownBy(() -> {
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(Long.MAX_VALUE, blocker);
            } else {
                bucket.asBlocking().consume(Long.MAX_VALUE, blocker);
            }
        }).isInstanceOf(IllegalArgumentException.class);
        assertThat(blocker.getParkedNanos()).isEqualTo(100_000_000);
    }

    @ParameterizedTest
    @MethodSource("typeVerboseCases")
    void testListenerForBlockingConsume(TypeVerboseCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean verbose = testCase.verbose();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        Bucket bucket = type.createBucket(configuration, clock, listener);

        if (verbose) {
            bucket.asBlocking().asVerbose().consume(9, blocker);
        } else {
            bucket.asBlocking().consume(9, blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getParkedNanos()).isEqualTo(0);
        assertThat(listener.beforeParkingNanos).isEqualTo(0);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        if (verbose) {
            bucket.asBlocking().asVerbose().consume(2, blocker);
        } else {
            bucket.asBlocking().consume(2, blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(11);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getParkedNanos()).isEqualTo(100_000_000);
        assertThat(listener.beforeParkingNanos).isEqualTo(100_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> {
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(1, blocker);
            } else {
                bucket.asBlocking().consume(1, blocker);
            }
        }).isInstanceOf(InterruptedException.class);
        assertThat(listener.getConsumed()).isEqualTo(12);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getParkedNanos()).isEqualTo(100_000_000);
        assertThat(listener.beforeParkingNanos).isEqualTo(200_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("typeVerboseCases")
    void testForBlockingConsumeUninterruptibly(TypeVerboseCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean verbose = testCase.verbose();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        Bucket bucket = type.createBucket(configuration, clock);

        if (verbose) {
            bucket.asBlocking().asVerbose().consumeUninterruptibly(9, blocker);
        } else {
            bucket.asBlocking().consumeUninterruptibly(9, blocker);
        }
        assertThat(blocker.getParkedNanos()).isEqualTo(0);

        if (verbose) {
            bucket.asBlocking().asVerbose().consumeUninterruptibly(2, blocker);
        } else {
            bucket.asBlocking().consumeUninterruptibly(2, blocker);
        }
        assertThat(blocker.getParkedNanos()).isEqualTo(100_000_000);

        Thread.currentThread().interrupt();
        if (verbose) {
            bucket.asBlocking().asVerbose().consumeUninterruptibly(1, blocker);
        } else {
            bucket.asBlocking().consumeUninterruptibly(1, blocker);
        }
        assertThat(Thread.interrupted()).isTrue();
        assertThat(blocker.getParkedNanos()).isEqualTo(200_000_000);

        assertThatThrownBy(() -> {
            if (verbose) {
                bucket.asBlocking().asVerbose().consumeUninterruptibly(Long.MAX_VALUE, blocker);
            } else {
                bucket.asBlocking().consumeUninterruptibly(Long.MAX_VALUE, blocker);
            }
        }).isInstanceOf(IllegalArgumentException.class);
        assertThat(blocker.getParkedNanos()).isEqualTo(200_000_000);
    }

    @ParameterizedTest
    @MethodSource("typeVerboseCases")
    void testListenerForBlockingConsumeUninterruptibly(TypeVerboseCase testCase) throws Exception {
        BucketType type = testCase.type();
        boolean verbose = testCase.verbose();

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        Bucket bucket = type.createBucket(configuration, clock, listener);

        if (verbose) {
            bucket.asBlocking().asVerbose().consume(9, blocker);
        } else {
            bucket.asBlocking().consume(9, blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(9);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getParkedNanos()).isEqualTo(0);
        assertThat(listener.beforeParkingNanos).isEqualTo(0);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        if (verbose) {
            bucket.asBlocking().asVerbose().consumeUninterruptibly(2, blocker);
        } else {
            bucket.asBlocking().consumeUninterruptibly(2, blocker);
        }
        assertThat(listener.getConsumed()).isEqualTo(11);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getParkedNanos()).isEqualTo(100_000_000);
        assertThat(listener.beforeParkingNanos).isEqualTo(100_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(0);

        Thread.currentThread().interrupt();
        if (verbose) {
            bucket.asBlocking().asVerbose().consumeUninterruptibly(1, blocker);
        } else {
            bucket.asBlocking().consumeUninterruptibly(1, blocker);
        }
        assertThat(Thread.interrupted()).isTrue();
        assertThat(listener.getConsumed()).isEqualTo(12);
        assertThat(listener.getRejected()).isEqualTo(0);
        assertThat(listener.getParkedNanos()).isEqualTo(200_000_000);
        assertThat(listener.beforeParkingNanos).isEqualTo(200_000_000);
        assertThat(listener.getInterrupted()).isEqualTo(0);
    }

}
