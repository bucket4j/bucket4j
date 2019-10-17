/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
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

import io.github.bucket4j.mock.*
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.TimeUnit

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS

class BlockingBucketSpecification extends Specification {

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep when trying to consuming #toConsume tokens from Bucket #configuration"(
            int n, long requiredSleep, long toConsume, BucketConfiguration configuration) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean uniterruptible : [true, false]) {
                for (boolean limitAsDuration: [true, false]) {
                    TimeMeterMock meter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(configuration, meter)
                    BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                    if (uniterruptible) {
                        if (limitAsDuration) {
                            bucket.asBlocking().tryConsumeUninterruptibly(toConsume, Duration.ofHours(1), sleepStrategy)
                        } else {
                            bucket.asBlocking().tryConsumeUninterruptibly(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                        }
                    } else {
                        if (limitAsDuration) {
                            bucket.asBlocking().tryConsume(toConsume, Duration.ofHours(1), sleepStrategy)
                        } else {
                            bucket.asBlocking().tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                        }
                    }
                    assert sleepStrategy.parkedNanos == requiredSleep
                }
            }
        }
        where:
        n | requiredSleep | toConsume | configuration
        1 |      10       |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0)).build()
        2 |       0       |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
        3 |    9990       |  1000     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to synchronous consume #toConsume tokens with limit #sleepLimit from Bucket #builder"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, BucketConfiguration configuration) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean uniterruptible : [true, false]) {
                    for (boolean limitAsDuration: [true, false]) {
                        TimeMeterMock meter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(configuration, meter)
                        BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                        if (uniterruptible) {
                            if (limitAsDuration) {
                                assert bucket.asBlocking().tryConsumeUninterruptibly(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy) == requiredResult
                            } else {
                                assert bucket.asBlocking().tryConsumeUninterruptibly(toConsume, sleepLimit, sleepStrategy) == requiredResult
                            }
                        } else {
                            if (limitAsDuration) {
                                assert bucket.asBlocking().tryConsume(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy) == requiredResult
                            } else {
                                assert bucket.asBlocking().tryConsume(toConsume, sleepLimit, sleepStrategy) == requiredResult
                            }
                        }
                        assert sleepStrategy.parkedNanos == requiredSleep
                    }
                }
            }
        where:
            n | requiredSleep | requiredResult | toConsume | sleepLimit |  configuration
            1 |      10       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0)).build()
            2 |      10       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0)).build()
            3 |       0       |     true       |     1     |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
            4 |       0       |     false      |   1000    |     11     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
            5 |      40       |     true       |     5     |     40     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
            6 |      40       |     true       |     5     |     41     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
            6 |       0       |     false      |     5     |     39     |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1)).build()
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    def "Should throw InterruptedException when thread interrupted during waiting for token refill"() {
        expect:
        for (TimeMeter meter : [SYSTEM_MILLISECONDS, TimeMeter.SYSTEM_NANOTIME]) {
            Bucket bucket = Bucket.builder()
                    .withCustomTimePrecision(meter)
                    .addLimit(Bandwidth.simple(1, Duration.ofMinutes(1)).withInitialTokens(0))
                    .build()

            Thread.currentThread().interrupt()
            InterruptedException thrown
            try {
                bucket.asBlocking().tryConsume(1, TimeUnit.HOURS.toNanos(1000), BlockingStrategy.PARKING)
            } catch (InterruptedException e) {
                thrown = e
            }
            assert thrown != null

            thrown = null
            Thread.currentThread().interrupt()
            try {
                bucket.asBlocking().tryConsume(1, TimeUnit.HOURS.toNanos(1), BlockingStrategy.PARKING)
            } catch (InterruptedException e) {
                thrown = e
            }
            assert thrown != null
        }
    }

    @Unroll
    def "#type test listener for blocking consume"(BucketType type) {
        setup:
            TimeMeterMock clock = new TimeMeterMock()
            BlockingStrategyMock blocker = new BlockingStrategyMock(clock)

            def config = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
                    .build()
            Bucket bucket = type.createBucket(config, clock)

        when:
            bucket.asBlocking().consume(9, blocker)
        then:
            blocker.parkedNanos == 0

        when:
            bucket.asBlocking().consume(2, blocker)
        then:
            blocker.parkedNanos == 100_000_000

        when:
            Thread.currentThread().interrupt()
            bucket.asBlocking().consume(1, blocker)
        then:
            thrown(InterruptedException)
            !Thread.interrupted()
            blocker.parkedNanos == 100_000_000
            blocker.atemptToParkNanos == 200_000_000

        when:
            bucket.asBlocking().consume(Long.MAX_VALUE, blocker)
        then:
            thrown(IllegalStateException)
            blocker.parkedNanos == 100_000_000

        where:
            type << BucketType.values()
    }

    @Unroll
    def "#type test listener for blocking consumeUninterruptibly"(BucketType type) {
        setup:
            TimeMeterMock clock = new TimeMeterMock()
            BlockingStrategyMock blocker = new BlockingStrategyMock(clock)
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
                    .build()
            Bucket bucket = type.createBucket(configuration, clock)

        when:
            bucket.asBlocking().consumeUninterruptibly(9, blocker)
        then:
            blocker.parkedNanos == 0

        when:
            bucket.asBlocking().consumeUninterruptibly(2, blocker)
        then:
            blocker.parkedNanos == 100_000_000

        when:
            Thread.currentThread().interrupt()
            bucket.asBlocking().consumeUninterruptibly(1, blocker)
        then:
            Thread.interrupted()
            blocker.parkedNanos == 200_000_000

        when:
            bucket.asBlocking().consumeUninterruptibly(Long.MAX_VALUE, blocker)
        then:
            thrown(IllegalStateException)
            blocker.parkedNanos == 200_000_000

        where:
            type << BucketType.values()
    }

}
