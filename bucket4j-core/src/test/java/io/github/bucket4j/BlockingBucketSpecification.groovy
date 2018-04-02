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
import io.github.bucket4j.mock.SchedulerMock
import io.github.bucket4j.mock.TimeMeterMock;
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.TimeUnit

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS;

class BlockingBucketSpecification extends Specification {

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep when trying to consuming #toConsume tokens from Bucket #builder"(
            int n, long requiredSleep, long toConsume, AbstractBucketBuilder builder) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                for (boolean uniterruptible : [true, false]) {
                    for (boolean limitAsDuration: [true, false]) {
                        TimeMeterMock meter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(builder, meter)
                        if (sync) {
                            BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                            if (uniterruptible) {
                                if (limitAsDuration) {
                                    bucket.asScheduler().tryConsumeUninterruptibly(toConsume, Duration.ofHours(1), sleepStrategy)
                                } else {
                                    bucket.asScheduler().tryConsumeUninterruptibly(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                                }
                            } else {
                                if (limitAsDuration) {
                                    bucket.asScheduler().tryConsume(toConsume, Duration.ofHours(1), sleepStrategy)
                                } else {
                                    bucket.asScheduler().tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                                }
                            }
                            assert sleepStrategy.sleeped == requiredSleep
                        } else {
                            SchedulerMock scheduler = new SchedulerMock()
                            bucket.asAsync().tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), scheduler).get()
                            assert scheduler.acummulatedDelayNanos == requiredSleep
                        }
                    }
                }
            }
        }
        where:
        n | requiredSleep | toConsume | builder
        1 |      10       |     1     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
        2 |       0       |     1     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
        3 |    9990       |  1000     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to synchronous consume #toConsume tokens with limit #sleepLimit from Bucket #builder"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, AbstractBucketBuilder builder) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                for (boolean uniterruptible : [true, false]) {
                    for (boolean limitAsDuration: [true, false]) {
                        TimeMeterMock meter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(builder, meter)
                        if (sync) {
                            BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                            if (uniterruptible) {
                                if (limitAsDuration) {
                                    assert bucket.asScheduler().tryConsumeUninterruptibly(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy) == requiredResult
                                } else {
                                    assert bucket.asScheduler().tryConsumeUninterruptibly(toConsume, sleepLimit, sleepStrategy) == requiredResult
                                }
                            } else {
                                if (limitAsDuration) {
                                    assert bucket.asScheduler().tryConsume(toConsume, Duration.ofNanos(sleepLimit), sleepStrategy) == requiredResult
                                } else {
                                    assert bucket.asScheduler().tryConsume(toConsume, sleepLimit, sleepStrategy) == requiredResult
                                }
                            }
                            assert sleepStrategy.sleeped == requiredSleep
                        } else {
                            SchedulerMock scheduler = new SchedulerMock()
                            assert bucket.asAsync().tryConsume(toConsume, sleepLimit, scheduler).get() == requiredResult
                            assert scheduler.acummulatedDelayNanos == requiredSleep
                        }
                    }
                }
            }
        }
        where:
        n | requiredSleep | requiredResult | toConsume | sleepLimit |  builder
        1 |      10       |     true       |     1     |     11     |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
        2 |      10       |     true       |     1     |     11     |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
        3 |       0       |     true       |     1     |     11     |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
        4 |       0       |     false      |   1000    |     11     |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
        5 |      40       |     true       |     5     |     40     |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
        6 |      40       |     true       |     5     |     41     |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
        6 |       0       |     false      |     5     |     39     |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    def "Should throw InterruptedException when thread interrupted during waiting for token refill"() {
        expect:
        for (TimeMeter meter : [SYSTEM_MILLISECONDS, TimeMeter.SYSTEM_NANOTIME]) {
            Bucket bucket = Bucket4j.builder()
                    .withCustomTimePrecision(meter)
                    .addLimit(Bandwidth.simple(1, Duration.ofMinutes(1)).withInitialTokens(0))
                    .build()

            Thread.currentThread().interrupt()
            InterruptedException thrown
            try {
                bucket.asScheduler().tryConsume(1, TimeUnit.HOURS.toNanos(1000), BlockingStrategy.PARKING)
            } catch (InterruptedException e) {
                thrown = e
            }
            assert thrown != null

            thrown = null
            Thread.currentThread().interrupt()
            try {
                bucket.asScheduler().tryConsume(1, TimeUnit.HOURS.toNanos(1), BlockingStrategy.PARKING)
            } catch (InterruptedException e) {
                thrown = e
            }
            assert thrown != null
        }
    }

}
