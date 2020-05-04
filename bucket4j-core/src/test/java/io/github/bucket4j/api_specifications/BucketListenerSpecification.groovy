
package io.github.bucket4j.api_specifications

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.SimpleBucketListener
import io.github.bucket4j.distributed.AsyncBucket
import io.github.bucket4j.mock.BlockingStrategyMock
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.SchedulerMock
import io.github.bucket4j.mock.TimeMeterMock
import io.github.bucket4j.util.PipeGenerator
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class BucketListenerSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    BlockingStrategyMock blocker = new BlockingStrategyMock(clock)
    SimpleBucketListener listener = new SimpleBucketListener()
	SchedulerMock scheduler = new SchedulerMock(clock)

    BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build()

    @Unroll
    def "#type bucket created by toListenable should share tokens with source bucket"(BucketType type) {
        setup:
        Bucket sourceBucket = type.createBucket(configuration, clock);
            Bucket listenableBucket = sourceBucket.toListenable(listener)

        when:
            sourceBucket.tryConsume(9)
        then:
            sourceBucket.getAvailableTokens() == 1
            listenableBucket.getAvailableTokens() == 1

        when:
            listenableBucket.tryConsume(1)
        then:
            sourceBucket.getAvailableTokens() == 0
            listenableBucket.getAvailableTokens() == 0

        expect:
            !sourceBucket.tryConsume(1)
            !listenableBucket.tryConsume(1)

        where:
            type << BucketType.values()
    }

    // =========== Sync cases ================================
    @Unroll
    def "#type verbose=#verbose test listener for tryConsume"(BucketType type, boolean verbose) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            if (!verbose) {
                bucket.tryConsume(9)
            } else {
                bucket.asVerbose().tryConsume(9)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsume(6)
            } else {
                bucket.asVerbose().tryConsume(6)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    @Unroll
    def "#type test listener for blocking tryConsume"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asBlocking().tryConsume(1, Duration.ofSeconds(1), blocker)
        then:
            thrown(InterruptedException)
            listener.getConsumed() == 12
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 1

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for blocking tryConsumeUninterruptibly"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asBlocking().tryConsumeUninterruptibly(1, Duration.ofSeconds(1), blocker)
            Thread.interrupted()
        then:
            listener.getConsumed() == 12
            listener.getRejected() == 1000
            listener.getParkedNanos() == 200_000_000
            listener.getInterrupted() == 0

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for blocking consume"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            bucket.asBlocking().consume(9, blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().consume(2, blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 0
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asBlocking().consume(1, blocker)
        then:
            thrown(InterruptedException)
            listener.getConsumed() == 12
            listener.getRejected() == 0
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 1

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for blocking consumeUninterruptibly"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            bucket.asBlocking().consume(9, blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asBlocking().consumeUninterruptibly(2, blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 0
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asBlocking().consumeUninterruptibly(1, blocker)
        then:
            Thread.interrupted()
            listener.getConsumed() == 12
            listener.getRejected() == 0
            listener.getParkedNanos() == 200_000_000
            listener.getInterrupted() == 0

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type verbose=#verbose test listener for tryConsumeAsMuchAsPossible"(BucketType type, boolean verbose) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible()
            } else {
                bucket.asVerbose().tryConsumeAsMuchAsPossible()
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible()
            } else {
                bucket.asVerbose().tryConsumeAsMuchAsPossible()
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    @Unroll
    def "#type verbose=#verbose test listener for tryConsumeAsMuchAsPossible with limit"(BucketType type, boolean verbose) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible(8)
            } else {
                bucket.asVerbose().tryConsumeAsMuchAsPossible(8)
            }
        then:
            listener.getConsumed() == 8
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible(8)
            } else {
                bucket.asVerbose().tryConsumeAsMuchAsPossible(8)
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible(3)
            } else {
                bucket.asVerbose().tryConsumeAsMuchAsPossible(3)
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    @Unroll
    def "#type verbose=#verbose test listener for tryConsumeAndReturnRemaining"(BucketType type, boolean verbose) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        when:
            if (!verbose) {
                bucket.tryConsumeAndReturnRemaining(9)
            } else {
                bucket.asVerbose().tryConsumeAndReturnRemaining(9)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAndReturnRemaining(6)
            } else {
                bucket.asVerbose().tryConsumeAndReturnRemaining(6)
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    // =========== Async cases ================================
    @Unroll
    def "#type verbose=#verbose test listener for async tryConsume"(BucketType type, boolean verbose) {
        setup:
            AsyncBucket bucket = type.createAsyncBucket(configuration, clock).toListenable(listener)

        when:
            if (!verbose) {
                bucket.tryConsume(9).get()
            } else {
                bucket.asAsync().asVerbose().tryConsume(9).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsume(6).get()
            } else {
                bucket.asAsync().asVerbose().tryConsume(6).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.withAsyncSupport() as List, [false, true])
    }

	@Unroll
    def "#type test listener for async scheduled tryConsume"(BucketType type) {
        setup:
            AsyncBucket bucket = type.createAsyncBucket(configuration, clock).toListenable(listener)

        when:
            bucket.asScheduler().tryConsume(9, Duration.ofSeconds(1).toNanos(), scheduler)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().tryConsume(1000, Duration.ofSeconds(1).toNanos(), scheduler)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().tryConsume(2, Duration.ofSeconds(1).toNanos(), scheduler)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
			listener.getDelayedNanos() == 100_000_000
            listener.getInterrupted() == 0

        where:
            type << BucketType.withAsyncSupport()
    }


    @Unroll
    def "#type test listener for async delayed consume"(BucketType type) {
        setup:
            AsyncBucket bucket = type.createAsyncBucket(configuration, clock).toListenable(listener)

        when:
            bucket.asScheduler().consume(9, scheduler)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().consume(2, scheduler)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 0
            listener.getDelayedNanos() == 100_000_000
            listener.getInterrupted() == 0

        where:
            type << BucketType.withAsyncSupport()
    }

    @Unroll
    def "#type verbose=#verbose test listener for async tryConsumeAsMuchAsPossible"(BucketType type, boolean verbose) {
        setup:
            AsyncBucket bucket = type.createAsyncBucket(configuration, clock).toListenable(listener)

        when:
            if (!verbose) {
                bucket.asAsync().tryConsumeAsMuchAsPossible().get()
            } else {
                bucket.asAsync().asVerbose().tryConsumeAsMuchAsPossible().get()
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.asAsync().tryConsumeAsMuchAsPossible().get()
            } else {
                bucket.asAsync().asVerbose().tryConsumeAsMuchAsPossible().get()
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.withAsyncSupport() as List, [false, true])
    }

    @Unroll
    def "#type verbose=#verbose test listener for async tryConsumeAsMuchAsPossible with limit"(BucketType type, boolean verbose) {
        setup:
            AsyncBucket bucket = type.createAsyncBucket(configuration, clock).toListenable(listener)

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible(8).get()
            } else {
                bucket.asAsync().asVerbose().tryConsumeAsMuchAsPossible(8).get()
            }
        then:
            listener.getConsumed() == 8
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible(8).get()
            } else {
                bucket.asAsync().asVerbose().tryConsumeAsMuchAsPossible(8).get()
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible(3).get()
            } else {
                bucket.asAsync().asVerbose().tryConsumeAsMuchAsPossible(3).get()
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.withAsyncSupport() as List, [false, true])
    }

	@Unroll
    def "#type verbose=#verbose test listener for async tryConsumeAndReturnRemaining"(BucketType type, boolean verbose) {
        setup:
            AsyncBucket bucket = type.createAsyncBucket(configuration, clock).toListenable(listener)

        when:
            if (!verbose) {
                bucket.tryConsumeAndReturnRemaining(9).get()
            } else {
                bucket.asAsync().asVerbose().tryConsumeAndReturnRemaining(9).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAndReturnRemaining(6).get()
            } else {
                bucket.asAsync().asVerbose().tryConsumeAndReturnRemaining(6).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.withAsyncSupport() as List, [false, true])
    }

}
