/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j

import io.github.bucket4j.mock.BlockingStrategyMock
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.SchedulerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class BucketListenerSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    BlockingStrategyMock blocker = new BlockingStrategyMock(clock)
    SimpleBucketListener listener = new SimpleBucketListener()
	SchedulerMock scheduler = new SchedulerMock(clock)

    AbstractBucketBuilder builder = Bucket4j.builder()
            .withCustomTimePrecision(clock)
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))

    // =========== Sync cases ================================
    @Unroll
    def "#type test listener for tryConsume"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.tryConsume(9)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            bucket.tryConsume(6)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for blocking tryConsume"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
           bucket.asScheduler().tryConsume(9, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().tryConsume(1000, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().tryConsume(2, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asScheduler().tryConsume(1, Duration.ofSeconds(1), blocker)
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
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.asScheduler().tryConsume(9, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().tryConsume(1000, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().tryConsume(2, Duration.ofSeconds(1), blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asScheduler().tryConsumeUninterruptibly(1, Duration.ofSeconds(1), blocker)
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
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.asScheduler().consume(9, blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().consume(2, blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 0
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asScheduler().consume(1, blocker)
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
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.asScheduler().consume(9, blocker)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getParkedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asScheduler().consumeUninterruptibly(2, blocker)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 0
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 0

        when:
            Thread.currentThread().interrupt()
            bucket.asScheduler().consumeUninterruptibly(1, blocker)
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
    def "#type test listener for tryConsumeAsMuchAsPossible"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.tryConsumeAsMuchAsPossible()
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        when:
            bucket.tryConsumeAsMuchAsPossible()
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for tryConsumeAsMuchAsPossible with limit"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.tryConsumeAsMuchAsPossible(8)
        then:
            listener.getConsumed() == 8
            listener.getRejected() == 0

        when:
            bucket.tryConsumeAsMuchAsPossible(8)
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        when:
            bucket.tryConsumeAsMuchAsPossible(3)
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for tryConsumeAndReturnRemaining"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.tryConsumeAndReturnRemaining(9)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            bucket.tryConsumeAndReturnRemaining(6)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            type << BucketType.values()
    }

    // =========== Async cases ================================
    @Unroll
    def "#type test listener for async tryConsume"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.asAsync().tryConsume(9).get()
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            bucket.asAsync().tryConsume(6).get()
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            type << BucketType.values()
    }

	@Unroll
    def "#type test listener for async scheduled tryConsume"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.asAsyncScheduler().tryConsume(9, Duration.ofSeconds(1).toNanos(), scheduler)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asAsyncScheduler().tryConsume(1000, Duration.ofSeconds(1).toNanos(), scheduler)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 1000
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asAsyncScheduler().tryConsume(2, Duration.ofSeconds(1).toNanos(), scheduler)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 1000
			listener.getDelayedNanos() == 100_000_000
            listener.getInterrupted() == 0

        where:
            type << BucketType.values()
    }


    @Unroll
    def "#type test listener for async delayed consume"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.asAsyncScheduler().consume(9, scheduler)
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0
            listener.getDelayedNanos() == 0
            listener.getInterrupted() == 0

        when:
            bucket.asAsyncScheduler().consume(2, scheduler)
        then:
            listener.getConsumed() == 11
            listener.getRejected() == 0
            listener.getDelayedNanos() == 100_000_000
            listener.getInterrupted() == 0

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for async tryConsumeAsMuchAsPossible"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.asAsync().tryConsumeAsMuchAsPossible().get()
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        when:
            bucket.asAsync().tryConsumeAsMuchAsPossible().get()
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        where:
            type << BucketType.values()
    }

	  @Unroll
    def "#type test listener for async tryConsumeAsMuchAsPossible with limit"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.asAsync().tryConsumeAsMuchAsPossible(8).get()
        then:
            listener.getConsumed() == 8
            listener.getRejected() == 0

        when:
            bucket.asAsync().tryConsumeAsMuchAsPossible(8).get()
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        when:
            bucket.asAsync().tryConsumeAsMuchAsPossible(3).get()
        then:
            listener.getConsumed() == 10
            listener.getRejected() == 0

        where:
            type << BucketType.values()
    }

	@Unroll
    def "#type test listener for async tryConsumeAndReturnRemaining"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock).toListenable(listener)

        when:
            bucket.asAsync().tryConsumeAndReturnRemaining(9).get()
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 0

        when:
            bucket.asAsync().tryConsumeAndReturnRemaining(6).get()
        then:
            listener.getConsumed() == 9
            listener.getRejected() == 6

        where:
            type << BucketType.values()
    }

}
