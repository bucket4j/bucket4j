package io.github.bucket4j.api_specifications.scheduler

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.SimpleBucketListener
import io.github.bucket4j.distributed.AsyncBucket
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.SchedulerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.ExecutionException

class BlockingTryConsumeWithSchedulerSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    SimpleBucketListener listener = new SimpleBucketListener()
    SchedulerMock scheduler = new SchedulerMock(clock)

    @Unroll
    def "#type test for async delayed consume"(BucketType type) {
        setup:
            def configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build()
            AsyncBucket bucket = type.createAsyncBucket(configuration, clock)

        when:
            bucket.asScheduler().consume(9, scheduler).get()
        then:
            scheduler.acummulatedDelayNanos == 0

        when:
            bucket.asScheduler().consume(2, scheduler).get()
        then:
            scheduler.acummulatedDelayNanos == 100_000_000

        when:
            bucket.asScheduler().consume(Long.MAX_VALUE, scheduler).get()
        then:
            ExecutionException ex = thrown(ExecutionException)
            ex.getCause().class == IllegalArgumentException

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for async delayed consume"(BucketType type) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
                .build()
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
            type << BucketType.values()
    }

}
