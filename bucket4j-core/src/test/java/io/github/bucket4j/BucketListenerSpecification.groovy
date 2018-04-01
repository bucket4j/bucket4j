/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j

import io.github.bucket4j.mock.BlockingStrategyMock
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

// TODO need to extend per each local and remote bucket type
class BucketListenerSpecification extends Specification {

    TimeMeterMock clock = new TimeMeterMock()
    BlockingStrategyMock blocker = new BlockingStrategyMock(clock)
    SimpleBucketListener listener = new SimpleBucketListener()

    AbstractBucketBuilder builder = Bucket4j.builder()
            .withListener(listener)
            .withCustomTimePrecision(clock)
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))

    // =========== Sync cases ================================
    @Unroll
    def "#type test listener for tryConsume"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock)

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
            Bucket bucket = type.createBucket(builder, clock)

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
            InterruptedException ex = thrown()
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
            Bucket bucket = type.createBucket(builder, clock)

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
            Bucket bucket = type.createBucket(builder, clock)

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
            InterruptedException ex = thrown()
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
            Bucket bucket = type.createBucket(builder, clock)

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
            bucket.asBlocking().consume(1, blocker)
            Thread.interrupted()
        then:
            InterruptedException ex = thrown()
            listener.getConsumed() == 12
            listener.getRejected() == 0
            listener.getParkedNanos() == 100_000_000
            listener.getInterrupted() == 1

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for tryConsumeAsMuchAsPossible"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock)

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
            Bucket bucket = type.createBucket(builder, clock)

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
            Bucket bucket = type.createBucket(builder, clock)

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
            Bucket bucket = type.createBucket(builder, clock)

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

//    def "#type test listener for blocking tryConsume"(BucketType type) {
//        setup:
//            Bucket bucket = type.createBucket(builder, clock)
//
//        when:
//            bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker)
//        then:
//            listener.getConsumed() == 9
//            listener.getRejected() == 0
//            listener.getParkedNanos() == 0
//            listener.getInterrupted() == 0
//
//        when:
//            bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker)
//        then:
//            listener.getConsumed() == 9
//            listener.getRejected() == 1000
//            listener.getParkedNanos() == 0
//            listener.getInterrupted() == 0
//
//        when:
//            bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker)
//        then:
//            listener.getConsumed() == 11
//            listener.getRejected() == 1000
//            listener.getParkedNanos() == 100_000_000
//            listener.getInterrupted() == 0
//
//        when:
//            Thread.currentThread().interrupt()
//            bucket.asBlocking().tryConsume(1, Duration.ofSeconds(1), blocker)
//        then:
//            InterruptedException ex = thrown()
//            listener.getConsumed() == 12
//            listener.getRejected() == 1000
//            listener.getParkedNanos() == 100_000_000
//            listener.getInterrupted() == 1
//
//        where:
//            type << BucketType.values()
//    }
//
//    def "#type test listener for blocking tryConsumeUninterruptibly"(BucketType type) {
//        setup:
//            Bucket bucket = type.createBucket(builder, clock)
//
//        when:
//            bucket.asBlocking().tryConsume(9, Duration.ofSeconds(1), blocker)
//        then:
//            listener.getConsumed() == 9
//            listener.getRejected() == 0
//            listener.getParkedNanos() == 0
//            listener.getInterrupted() == 0
//
//        when:
//            bucket.asBlocking().tryConsume(1000, Duration.ofSeconds(1), blocker)
//        then:
//            listener.getConsumed() == 9
//            listener.getRejected() == 1000
//            listener.getParkedNanos() == 0
//            listener.getInterrupted() == 0
//
//        when:
//            bucket.asBlocking().tryConsume(2, Duration.ofSeconds(1), blocker)
//        then:
//            listener.getConsumed() == 11
//            listener.getRejected() == 1000
//            listener.getParkedNanos() == 100_000_000
//            listener.getInterrupted() == 0
//
//        when:
//            Thread.currentThread().interrupt()
//            bucket.asBlocking().tryConsumeUninterruptibly(1, Duration.ofSeconds(1), blocker)
//            Thread.interrupted()
//        then:
//            listener.getConsumed() == 12
//            listener.getRejected() == 1000
//            listener.getParkedNanos() == 200_000_000
//            listener.getInterrupted() == 0
//
//        where:
//            type << BucketType.values()
//    }
//
//    def "#type test listener for blocking consume"(BucketType type) {
//        setup:
//            Bucket bucket = type.createBucket(builder, clock)
//
//        when:
//            bucket.asBlocking().consume(9, blocker)
//        then:
//            listener.getConsumed() == 9
//            listener.getRejected() == 0
//            listener.getParkedNanos() == 0
//            listener.getInterrupted() == 0
//
//        when:
//            bucket.asBlocking().consume(2, blocker)
//        then:
//            listener.getConsumed() == 11
//            listener.getRejected() == 0
//            listener.getParkedNanos() == 100_000_000
//            listener.getInterrupted() == 0
//
//        when:
//            Thread.currentThread().interrupt()
//            bucket.asBlocking().consume(1, blocker)
//        then:
//            InterruptedException ex = thrown()
//            listener.getConsumed() == 12
//            listener.getRejected() == 0
//            listener.getParkedNanos() == 100_000_000
//            listener.getInterrupted() == 1
//
//        where:
//            type << BucketType.values()
//    }


    @Unroll
    def "#type test listener for async tryConsumeAsMuchAsPossible"(BucketType type) {
        setup:
            Bucket bucket = type.createBucket(builder, clock)

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

//    def "#type test listener for tryConsumeAsMuchAsPossible with limit"(BucketType type) {
//        setup:
//            Bucket bucket = type.createBucket(builder, clock)
//
//        when:
//            bucket.tryConsumeAsMuchAsPossible(8)
//        then:
//            listener.getConsumed() == 8
//            listener.getRejected() == 0
//
//        when:
//            bucket.tryConsumeAsMuchAsPossible(8)
//        then:
//            listener.getConsumed() == 10
//            listener.getRejected() == 0
//
//        when:
//            bucket.tryConsumeAsMuchAsPossible(3)
//        then:
//            listener.getConsumed() == 10
//            listener.getRejected() == 0
//
//        where:
//            type << BucketType.values()
//    }
//
//    def "#type test listener for tryConsumeAndReturnRemaining"(BucketType type) {
//        setup:
//            Bucket bucket = type.createBucket(builder, clock)
//
//        when:
//            bucket.tryConsumeAndReturnRemaining(9)
//        then:
//            listener.getConsumed() == 9
//            listener.getRejected() == 0
//
//        when:
//            bucket.tryConsumeAndReturnRemaining(6)
//        then:
//            listener.getConsumed() == 9
//            listener.getRejected() == 6
//
//        where:
//            type << BucketType.values()
//    }



}
