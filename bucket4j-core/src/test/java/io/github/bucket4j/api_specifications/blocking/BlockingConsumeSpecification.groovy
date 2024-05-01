package io.github.bucket4j.api_specifications.blocking

import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.SimpleBucketListener
import io.github.bucket4j.mock.BlockingStrategyMock
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import io.github.bucket4j.util.PipeGenerator
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class BlockingConsumeSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    BlockingStrategyMock blocker = new BlockingStrategyMock(clock)

    SimpleBucketListener listener = new SimpleBucketListener() {
        long beforeParkingNanos;
        @Override
        void beforeParking(long nanos) {
            beforeParkingNanos += nanos
        }
    }

    @Unroll
    def "#type test for blocking consume verbose=#verbose"(BucketType type, boolean verbose) {
        setup:
            def configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            Bucket bucket = type.createBucket(configuration, clock)

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(9, blocker)
            } else {
                bucket.asBlocking().consume(9, blocker)
            }
        then:
            blocker.parkedNanos == 0

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(2, blocker)
            } else {
                bucket.asBlocking().consume(2, blocker)
            }
        then:
            blocker.parkedNanos == 100_000_000

        when:
            Thread.currentThread().interrupt()
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(1, blocker)
            } else {
                bucket.asBlocking().consume(1, blocker)
            }
        then:
            thrown(InterruptedException)
            !Thread.interrupted()
            blocker.parkedNanos == 100_000_000
            blocker.atemptToParkNanos == 200_000_000

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(Long.MAX_VALUE, blocker)
            } else {
                bucket.asBlocking().consume(Long.MAX_VALUE, blocker)
            }
        then:
            thrown(IllegalArgumentException)
            blocker.parkedNanos == 100_000_000

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    @Unroll
    def "#type test listener for blocking consume verbose=#verbose"(BucketType type, boolean verbose) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            Bucket bucket = type.createBucket(configuration, clock, listener)

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(9, blocker)
            } else {
                bucket.asBlocking().consume(9, blocker)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(2, blocker)
            } else {
                bucket.asBlocking().consume(2, blocker)
            }
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 0
            listener.getParkedNanos() == 100_000_000
            listener.beforeParkingNanos == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(1, blocker)
            } else {
                bucket.asBlocking().consume(1, blocker)
            }
        then:
            thrown(InterruptedException)
            listener.getConsumed() == 12
            listener.getRejected() == 0
            listener.getParkedNanos() == 100_000_000
            listener.beforeParkingNanos == 200_000_000
            listener.getInterrupted() == 1

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    @Unroll
    def "#type test for blocking consumeUninterruptibly verbose=#verbose"(BucketType type, boolean verbose) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            Bucket bucket = type.createBucket(configuration, clock)

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consumeUninterruptibly(9, blocker)
            } else {
                bucket.asBlocking().consumeUninterruptibly(9, blocker)
            }
        then:
            blocker.parkedNanos == 0

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consumeUninterruptibly(2, blocker)
            } else {
                bucket.asBlocking().consumeUninterruptibly(2, blocker)
            }
        then:
            blocker.parkedNanos == 100_000_000

        when:
            Thread.currentThread().interrupt()
            if (verbose) {
                bucket.asBlocking().asVerbose().consumeUninterruptibly(1, blocker)
            } else {
                bucket.asBlocking().consumeUninterruptibly(1, blocker)
            }
        then:
            Thread.interrupted()
            blocker.parkedNanos == 200_000_000

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consumeUninterruptibly(Long.MAX_VALUE, blocker)
            } else {
                bucket.asBlocking().consumeUninterruptibly(Long.MAX_VALUE, blocker)
            }
        then:
            thrown(IllegalArgumentException)
            blocker.parkedNanos == 200_000_000

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    @Unroll
    def "#type test listener for blocking consumeUninterruptibly verbose=#verbose"(BucketType type, boolean verbose) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            Bucket bucket = type.createBucket(configuration, clock, listener)

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consume(9, blocker)
            } else {
                bucket.asBlocking().consume(9, blocker)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.beforeParkingNanos == 0
            listener.getInterrupted() == 0

        when:
            if (verbose) {
                bucket.asBlocking().asVerbose().consumeUninterruptibly(2, blocker)
            } else {
                bucket.asBlocking().consumeUninterruptibly(2, blocker)
            }
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 0
            listener.getParkedNanos() == 100_000_000
            listener.beforeParkingNanos == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            if (verbose) {
                bucket.asBlocking().asVerbose().consumeUninterruptibly(1, blocker)
            } else {
                bucket.asBlocking().consumeUninterruptibly(1, blocker)
            }
        then:
            Thread.interrupted()
            listener.getConsumed() == 12
            listener.getRejected() == 0
            listener.getParkedNanos() == 200_000_000
            listener.beforeParkingNanos == 200_000_000
            listener.getInterrupted() == 0

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

}
