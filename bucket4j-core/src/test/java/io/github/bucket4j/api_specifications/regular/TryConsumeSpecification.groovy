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

class TryConsumeSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    SimpleBucketListener listener = new SimpleBucketListener()

    BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build()

    @Unroll
    def "#n Should return #requiredResult when trying to consume #toConsume tokens from Bucket #configuration"(
            int n, boolean requiredResult, long toConsume, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            println type
            def timeMeter = new TimeMeterMock(0)
            Bucket bucket = type.createBucket(configuration, timeMeter)
            assert bucket.tryConsume(toConsume) == requiredResult

            AsyncBucketProxy asyncBucket = type.createAsyncBucket(configuration, timeMeter)
            assert asyncBucket.tryConsume(toConsume).get() == requiredResult
        }
        where:
        n | requiredResult | toConsume | configuration
        1 |     false      |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0)).build()
        2 |      true      |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(1)).build()
    }

    @Unroll
    def "#type verbose=#verbose test listener for tryConsume"(BucketType type, boolean verbose) {
        setup:
            Bucket bucket = type.createBucket(configuration, clock).toListenable(listener)

        boolean consumed
        when:
            if (!verbose) {
                consumed = bucket.tryConsume(9)
            } else {
                consumed = bucket.asVerbose().tryConsume(9)
            }
        then:
            consumed
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
    def "#type verbose=#verbose test listener for async tryConsume"(BucketType type, boolean verbose) {
        setup:
            AsyncBucketProxy bucket = type.createAsyncBucket(configuration, clock).toListenable(listener)

        when:
            if (!verbose) {
                bucket.tryConsume(9).get()
            } else {
                bucket.asVerbose().tryConsume(9).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            if (!verbose) {
                bucket.tryConsume(6).get()
            } else {
                bucket.asVerbose().tryConsume(6).get()
            }
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            [type, verbose] << PipeGenerator.сartesianProduct(BucketType.values() as List, [false, true])
    }

}
