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

import io.github.bucket4j.grid.GridBucket
import io.github.bucket4j.grid.GridProxy
import io.github.bucket4j.grid.RecoveryStrategy
import io.github.bucket4j.local.LocalBucketBuilder
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.BlockingStrategyMock
import io.github.bucket4j.mock.GridProxyMock
import io.github.bucket4j.mock.SchedulerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS
import static io.github.bucket4j.grid.RecoveryStrategy.*

class BucketSpecification extends Specification {

    @Unroll
    def "#n Should return #requiredResult when trying to consume single token from Bucket #builder"(
            int n, boolean requiredResult, ConfigurationBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    def timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    if (sync) {
                        assert bucket.tryConsume(1) == requiredResult
                    } else {
                        assert bucket.asAsync().tryConsume(1).get() == requiredResult
                    }
                }
            }
        where:
            n | requiredResult |  builder
            1 |     false      |  Bucket4j.builder().addLimit(0, Bandwidth.simple(10, Duration.ofMinutes(100)))
            2 |      true      |  Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofMinutes(100)))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consume #toConsume tokens from Bucket #builder"(
            int n, boolean requiredResult, long toConsume, ConfigurationBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    def timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    if (sync) {
                        assert bucket.tryConsume(toConsume) == requiredResult
                    } else {
                        assert bucket.asAsync().tryConsume(toConsume).get() == requiredResult
                    }
                }
            }
        where:
            n | requiredResult | toConsume | builder
            1 |     false      |     1     | Bucket4j.builder().addLimit(0, Bandwidth.simple(10, Duration.ofMinutes(100)))
            2 |      true      |     1     | Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofMinutes(100)))
    }

    @Unroll
    def "#n tryConsumeAndReturnRemaining specification"(int n, long toConsume, boolean result, long expectedRemaining, long expectedWait, ConfigurationBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    ConsumptionProbe probe
                    if (sync) {
                        probe = bucket.tryConsumeAndReturnRemaining(toConsume)
                    } else {
                        probe = bucket.asAsync().tryConsumeAndReturnRemaining(toConsume).get()
                    }
                    assert probe.consumed == result
                    assert probe.remainingTokens == expectedRemaining
                    assert probe.nanosToWaitForRefill == expectedWait
                }
            }
        where:
            n | toConsume | result  |  expectedRemaining | expectedWait | builder
            1 |    49     |   true  |           51       |       0      | Bucket4j.builder().addLimit(100, Bandwidth.simple(100, Duration.ofNanos(100)))
            2 |     1     |   true  |            0       |       0      | Bucket4j.builder().addLimit(1, Bandwidth.simple(100, Duration.ofNanos(100)))
            3 |    80     |   false |           70       |      10      | Bucket4j.builder().addLimit(70, Bandwidth.simple(100, Duration.ofNanos(100)))
            4 |    10     |   false |            0       |      10      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            5 |   120     |   false |           10       |     110      | Bucket4j.builder().addLimit(10, Bandwidth.simple(100, Duration.ofNanos(100)))
    }

    @Unroll
    def "#n Should return #requiredResult when consumeAsMuchAsPossible tokens from Bucket #builder"(
            int n, long requiredResult, ConfigurationBuilder builder) {
        expect:
            for (BucketType bucketType : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = bucketType.createBucket(builder, timeMeter)
                    if (sync) {
                        assert bucket.tryConsumeAsMuchAsPossible() == requiredResult
                    } else {
                        assert bucket.asAsync().tryConsumeAsMuchAsPossible().get() == requiredResult
                    }
                }
            }
        where:
            n | requiredResult | builder
            1 |        0       | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(0, Bandwidth.simple(10, Duration.ofMinutes(100)))
            2 |        2       | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(2, Bandwidth.simple(10, Duration.ofMinutes(100)))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consumeAsMuchAsPossible with limit #limit tokens from Bucket #builder"(
            int n, long requiredResult, long limit, ConfigurationBuilder builder) {
        expect:
            for (BucketType bucketType : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = bucketType.createBucket(builder, timeMeter)
                    if (sync) {
                        assert bucket.tryConsumeAsMuchAsPossible(limit) == requiredResult
                    } else {
                        assert bucket.asAsync().tryConsumeAsMuchAsPossible(limit).get() == requiredResult
                    }
                }
            }
        where:
            n | requiredResult |   limit   | builder
            1 |       4        |     5     | Bucket4j.builder().addLimit(4, Bandwidth.simple(10, Duration.ofMinutes(100)))
            2 |       5        |     5     | Bucket4j.builder().addLimit(5, Bandwidth.simple(10, Duration.ofMinutes(100)))
            3 |       5        |     5     | Bucket4j.builder().addLimit(6, Bandwidth.simple(10, Duration.ofMinutes(100)))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep when trying to consuming #toConsume tokens from Bucket #builder"(
            int n, long requiredSleep, long toConsume, ConfigurationBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean uniterruptible : [true, false]) {
                        TimeMeterMock meter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(builder, meter)
                        if (sync) {
                            BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                            if (uniterruptible) {
                                bucket.tryConsumeUninterruptibly(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
                            } else {
                                bucket.tryConsume(toConsume, TimeUnit.HOURS.toNanos(1), sleepStrategy)
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
        where:
            n | requiredSleep | toConsume | builder
            1 |      10       |     1     | Bucket4j.builder().addLimit(0, Bandwidth.simple(10, Duration.ofNanos(100)))
            2 |       0       |     1     | Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
            3 |    9990       |  1000     | Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to synchronous consume #toConsume tokens with limit #sleepLimit from Bucket #builder"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, ConfigurationBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    for (boolean uniterruptible : [true, false]) {
                        TimeMeterMock meter = new TimeMeterMock(0)
                        Bucket bucket = type.createBucket(builder, meter)
                        if (sync) {
                            BlockingStrategyMock sleepStrategy = new BlockingStrategyMock(meter)
                            if (uniterruptible) {
                                assert bucket.tryConsumeUninterruptibly(toConsume, sleepLimit, sleepStrategy) == requiredResult
                            } else {
                                assert bucket.tryConsume(toConsume, sleepLimit, sleepStrategy) == requiredResult
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
        where:
            n | requiredSleep | requiredResult | toConsume | sleepLimit |  builder
            1 |      10       |     true       |     1     |     11     |  Bucket4j.builder().addLimit(0, Bandwidth.simple(10, Duration.ofNanos(100)))
            2 |      10       |     true       |     1     |     11     |  Bucket4j.builder().addLimit(0, Bandwidth.simple(10, Duration.ofNanos(100)))
            3 |       0       |     true       |     1     |     11     |  Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
            4 |       0       |     false      |   1000    |     11     |  Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
            5 |      40       |     true       |     5     |     40     |  Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
            6 |      40       |     true       |     5     |     41     |  Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
            6 |       0       |     false      |     5     |     39     |  Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
    }

    @Unroll
    def "#n Add tokens spec"(
            int n, long tokensToAdd, long nanosIncrement, long requiredResult, ConfigurationBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    timeMeter.addTime(nanosIncrement)
                    if (sync) {
                        bucket.addTokens(tokensToAdd)
                    } else {
                        bucket.asAsync().addTokens(tokensToAdd).get()
                    }
                    assert bucket.createSnapshot().getAvailableTokens(bucket.configuration.bandwidths) == requiredResult
                }
            }
        where:
            n | tokensToAdd | nanosIncrement | requiredResult | builder
            1 |     49      |     50         |        99      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            2 |     50      |     50         |       100      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            3 |     50      |     0          |        50      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            4 |     120     |     0          |       100      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            5 |     120     |     110        |       100      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
    }

    @Unroll
    def "#n getAvailableTokens specification"(int n, long nanosSinceBucketCreation, long expectedTokens,  ConfigurationBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(builder, timeMeter)
                timeMeter.addTime(nanosSinceBucketCreation)
                assert bucket.getAvailableTokens() == expectedTokens
            }
        where:
            n | nanosSinceBucketCreation | expectedTokens |  builder
            1 |             49           |     50         | Bucket4j.builder().addLimit(1, Bandwidth.simple(100, Duration.ofNanos(100)))
            2 |             50           |     50         | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            3 |             50           |    100         | Bucket4j.builder().addLimit(70, Bandwidth.simple(100, Duration.ofNanos(100)))
            4 |              0           |      0         | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            5 |            120           |    100         | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    def "Should throw InterruptedException when thread interrupted during waiting for token refill"() {
        expect:
            for (TimeMeter meter : [SYSTEM_MILLISECONDS, TimeMeter.SYSTEM_NANOTIME]) {
                Bucket bucket = Bucket4j.builder()
                        .withCustomTimePrecision(meter)
                        .addLimit(0, Bandwidth.simple(1, Duration.ofMinutes(1)))
                        .build()

                Thread.currentThread().interrupt()
                InterruptedException thrown
                try {
                    bucket.tryConsume(1, TimeUnit.HOURS.toNanos(1000), BlockingStrategy.PARKING)
                } catch (InterruptedException e) {
                    thrown = e
                }
                assert thrown != null

                thrown = null
                Thread.currentThread().interrupt()
                try {
                    bucket.tryConsume(1, TimeUnit.HOURS.toNanos(1), BlockingStrategy.PARKING)
                } catch (InterruptedException e) {
                    thrown = e
                }
                assert thrown != null
            }
    }

    def "should complete future exceptionally if backend failed"() {
        setup:
            BucketConfiguration configuration = Bucket4j.configurationBuilder()
                                                    .addLimit(Bandwidth.simple(1, Duration.ofNanos(1)))
                                                    .buildConfiguration()
            GridProxyMock mockProxy = new GridProxyMock(SYSTEM_MILLISECONDS);
            Bucket bucket = GridBucket.createInitializedBucket("66", configuration, mockProxy, THROW_BUCKET_NOT_FOUND_EXCEPTION)
        when:
            mockProxy.setException(new RuntimeException())
            CompletableFuture<Boolean> future = bucket.asAsync().tryConsume(1)
        then:
            future.isCompletedExceptionally()
    }

    def "should complete future exceptionally if scheduler failed to schedule the task"() {
        setup:
            BucketConfiguration configuration = Bucket4j.configurationBuilder()
                    .addLimit(Bandwidth.simple(1, Duration.ofNanos(1)))
                    .buildConfiguration()
            GridProxyMock mockProxy = new GridProxyMock(SYSTEM_MILLISECONDS)
            SchedulerMock schedulerMock = new SchedulerMock()
            Bucket bucket = GridBucket.createInitializedBucket("66", configuration, mockProxy, THROW_BUCKET_NOT_FOUND_EXCEPTION)
        when:
            schedulerMock.setException(new RuntimeException())
            CompletableFuture<Boolean> future = bucket.asAsync().tryConsume(10, 100000, schedulerMock)
        then:
            future.isCompletedExceptionally()
    }

    def "check that toString does not fail"() {
        when:
            for (BucketType type : BucketType.values()) {
                for (TimeMeter meter : [SYSTEM_MILLISECONDS, SYSTEM_MILLISECONDS]) {
                    ConfigurationBuilder builder = Bucket4j.builder()
                            .addLimit(1, Bandwidth.simple(100, Duration.ofNanos(100)))
                    Bucket bucket = type.createBucket(builder, meter)
                    println bucket.toString()
                }
            }
        then:
            noExceptionThrown()
    }

}
