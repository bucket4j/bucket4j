package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.SimpleBucketListener
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import io.github.bucket4j.util.PipeGenerator
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class ConsumeAsMuchAsPossibleSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    SimpleBucketListener listener = new SimpleBucketListener()

    BucketConfiguration configuration = BucketConfiguration.builder()
        .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
        .build()

    @Unroll
    def "#n Should return #requiredResult when consumeAsMuchAsPossible tokens from Bucket #configuration"(int n, long requiredResult, BucketConfiguration configuration) {
        expect:
        for (BucketType bucketType : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0)
            Bucket bucket = bucketType.createBucket(configuration, timeMeter)
            assert bucket.tryConsumeAsMuchAsPossible() == requiredResult

            AsyncBucketProxy asyncBucket = bucketType.createAsyncBucket(configuration, timeMeter)
            assert asyncBucket.tryConsumeAsMuchAsPossible().get() == requiredResult
        }
        where:
        n | requiredResult | configuration
        1 |        0       | BucketConfiguration.builder().addLimit({it.capacity(10).refillGreedy(10, Duration.ofMinutes(100)).initialTokens(0)}).build()
        2 |        2       | BucketConfiguration.builder().addLimit({it.capacity(10).refillGreedy(10, Duration.ofMinutes(100)).initialTokens(2)}).build()
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consumeAsMuchAsPossible with limit #limit tokens from Bucket #configuration"(
            int n, long requiredResult, long limit, BucketConfiguration configuration) {
        expect:
        for (BucketType bucketType : BucketType.values()) {
            TimeMeterMock timeMeter = new TimeMeterMock(0)
            Bucket bucket = bucketType.createBucket(configuration, timeMeter)
            assert bucket.tryConsumeAsMuchAsPossible(limit) == requiredResult

            AsyncBucketProxy asyncBucket = bucketType.createAsyncBucket(configuration, timeMeter)
            assert asyncBucket.tryConsumeAsMuchAsPossible(limit).get() == requiredResult
        }
        where:
        n | requiredResult |   limit   | configuration
        1 |       4        |     5     | BucketConfiguration.builder().addLimit({it.capacity(10).refillGreedy(10, Duration.ofMinutes(100)).initialTokens(4)}).build()
        2 |       5        |     5     | BucketConfiguration.builder().addLimit({it.capacity(10).refillGreedy(10, Duration.ofMinutes(100)).initialTokens(5)}).build()
        3 |       5        |     5     | BucketConfiguration.builder().addLimit({it.capacity(10).refillGreedy(10, Duration.ofMinutes(100)).initialTokens(5)}).build()
    }

    @Unroll
    def "#type verbose=#verbose test listener for tryConsumeAsMuchAsPossible"(BucketType type, boolean verbose) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock, listener)

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
            Bucket bucket = type.createBucket(configuration, clock, listener)

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
    def "#type verbose=#verbose test listener for async tryConsumeAsMuchAsPossible"(BucketType type, boolean verbose) {
        setup:
        AsyncBucketProxy bucket = type.createAsyncBucket(configuration, clock, listener)

        when:
        if (!verbose) {
            bucket.tryConsumeAsMuchAsPossible().get()
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible().get()
        }
        then:
        listener.getConsumed() == 10
        listener.getRejected() == 0

        when:
        if (!verbose) {
            bucket.tryConsumeAsMuchAsPossible().get()
        } else {
            bucket.asVerbose().tryConsumeAsMuchAsPossible().get()
        }
        then:
        listener.getConsumed() == 10
        listener.getRejected() == 0

        where:
        [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

    @Unroll
    def "#type verbose=#verbose test listener for async tryConsumeAsMuchAsPossible with limit"(BucketType type, boolean verbose) {
        setup:
            AsyncBucketProxy bucket = type.createAsyncBucket(configuration, clock, listener)

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible(8).get()
            } else {
                bucket.asVerbose().tryConsumeAsMuchAsPossible(8).get()
            }
        then:
            listener.getConsumed() == 8
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible(8).get()
            } else {
                bucket.asVerbose().tryConsumeAsMuchAsPossible(8).get()
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsumeAsMuchAsPossible(3).get()
            } else {
                bucket.asVerbose().tryConsumeAsMuchAsPossible(3).get()
            }
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

}
