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

import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.TimeUnit

class BucketSpecification extends Specification {

    @Unroll
    def "#n Should return #requiredResult when trying to consume single token from Bucket #builder"(
            int n, boolean requiredResult, AbstractBucketBuilder builder) {
        expect:
            for (Bucket bucket : BucketType.createBuckets(builder)) {
                assert bucket.tryConsume(1) == requiredResult
            }
        where:
            n | requiredResult |  builder
            1 |     false      |  Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(0, Bandwidth.simple(10, Duration.ofMinutes(100)))
            2 |      true      |  Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(1, Bandwidth.simple(10, Duration.ofMinutes(100)))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consume #toConsume tokens from Bucket #builder"(
            int n, boolean requiredResult, long toConsume, AbstractBucketBuilder builder) {
        expect:
            for (Bucket bucket : BucketType.createBuckets(builder)) {
                assert bucket.tryConsume(toConsume) == requiredResult
            }
        where:
            n | requiredResult | toConsume | builder
            1 |     false      |     1     | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(0, Bandwidth.simple(10, Duration.ofMinutes(100)))
            2 |      true      |     1     | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(1, Bandwidth.simple(10, Duration.ofMinutes(100)))
    }

    @Unroll
    def "#n Should return #requiredResult when consumeAsMuchAsPossible tokens from Bucket #builder"(
            int n, long requiredResult, AbstractBucketBuilder builder) {
        expect:
            for (Bucket bucket : BucketType.createBuckets(builder)) {
                assert bucket.tryConsumeAsMuchAsPossible() == requiredResult
            }
        where:
            n | requiredResult | builder
            1 |        0       | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(0, Bandwidth.simple(10, Duration.ofMinutes(100)))
            2 |        2       | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(2, Bandwidth.simple(10, Duration.ofMinutes(100)))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consumeAsMuchAsPossible with limit #limit tokens from Bucket #builder"(
            int n, long requiredResult, long limit, AbstractBucketBuilder builder) {
        expect:
            for (Bucket bucket : BucketType.createBuckets(builder)) {
                assert bucket.tryConsumeAsMuchAsPossible(limit) == requiredResult
            }
        where:
            n | requiredResult |   limit   | builder
            1 |       4        |     5     | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(4, Bandwidth.simple(10, Duration.ofMinutes(100)))
            2 |       5        |     5     | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(5, Bandwidth.simple(10, Duration.ofMinutes(100)))
            3 |       5        |     5     | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(6, Bandwidth.simple(10, Duration.ofMinutes(100)))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep when trying to consuming #toConsume tokens from Bucket #builder"(
            int n, long requiredSleep, long toConsume, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean uniterruptible : Arrays.asList(false, true)) {
                    TimeMeterMock meter = new TimeMeterMock(0)
                    builder.withCustomTimePrecision(meter)
                    Bucket bucket = type.createBucket(builder)
                    if (uniterruptible) {
                        bucket.consumeUninterruptibly(toConsume)
                    } else {
                        bucket.consume(toConsume)
                    }
                    assert meter.sleeped == requiredSleep
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
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to consume single token with limit #sleepLimit from Bucket #builder"(
            int n, long requiredSleep, boolean requiredResult, long sleepLimit, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean uniterruptible : Arrays.asList(false, true)) {
                    TimeMeterMock meter = new TimeMeterMock(0)
                    builder.withCustomTimePrecision(meter)
                    Bucket bucket = type.createBucket(builder)

                    if (uniterruptible) {
                        bucket.consumeUninterruptibly(1, sleepLimit) == requiredResult
                    } else {
                        bucket.consume(1, sleepLimit) == requiredResult
                    }
                    assert meter.sleeped == requiredSleep
                }
            }
        where:
            n | requiredSleep | requiredResult | sleepLimit | builder
            1 |      10       |     true       |     11     | Bucket4j.builder().addLimit(0, Bandwidth.simple(10, Duration.ofNanos(100)))
            2 |       0       |     true       |     11     | Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to consume #toConsume tokens with limit #sleepLimit from Bucket #builder"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean uniterruptible : Arrays.asList(false, true)) {
                    TimeMeterMock meter = new TimeMeterMock(0)
                    builder.withCustomTimePrecision(meter)
                    Bucket bucket = type.createBucket(builder)

                    if (uniterruptible) {
                        bucket.consumeUninterruptibly(1, sleepLimit) == requiredResult
                    } else {
                        bucket.consume(1, sleepLimit) == requiredResult
                    }
                    assert meter.sleeped == requiredSleep
                }
            }
        where:
            n | requiredSleep | requiredResult | toConsume | sleepLimit |  builder
            1 |      10       |     true       |     1     |     11     |  Bucket4j.builder().addLimit(0, Bandwidth.simple(10, Duration.ofNanos(100)))
            2 |      10       |     false      |     1     |     11     |  Bucket4j.builder().addLimit(0, Bandwidth.simple(10, Duration.ofNanos(100)))
            3 |       0       |     true       |     1     |     11     |  Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
            4 |       0       |     false      |   1000    |     11     |  Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
            5 |       0       |     false      |     5     |     11     |  Bucket4j.builder().addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
    }

    @Unroll
    def "#n Add tokens spec"(
            int n, long tokensToAdd, long nanosIncrement, long requiredResult, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                TimeMeterMock mock = new TimeMeterMock(0)
                builder.withCustomTimePrecision(mock)
                Bucket bucket = type.createBucket(builder)
                mock.addTime(nanosIncrement)
                bucket.addTokens(tokensToAdd)
                assert bucket.createSnapshot().getAvailableTokens(bucket.configuration.bandwidths) == requiredResult
            }
        where:
            n | tokensToAdd | nanosIncrement | requiredResult | builder
            1 |     49      |     50         |        99      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            2 |     50      |     50         |       100      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            3 |     50      |     0          |        50      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            4 |     120     |     0          |       100      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
            5 |     120     |     110        |       100      | Bucket4j.builder().addLimit(0, Bandwidth.simple(100, Duration.ofNanos(100)))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    def "Should throw InterruptedException when thread interrupted during waiting for token refill"() {
        expect:
            for (BucketType type : BucketType.values()) {
                for (TimeMeter meter : [TimeMeter.SYSTEM_MILLISECONDS, TimeMeter.SYSTEM_NANOTIME]) {
                    Bucket bucket = Bucket4j.builder()
                            .withCustomTimePrecision(meter)
                            .addLimit(0, Bandwidth.simple(1, Duration.ofMinutes(1)))
                            .build()

                    Thread.currentThread().interrupt()
                    InterruptedException thrown
                    try {
                        bucket.consume(1)
                    } catch (InterruptedException e) {
                        thrown = e
                    }
                    assert thrown != null

                    thrown = null
                    Thread.currentThread().interrupt()
                    try {
                        bucket.consume(1, TimeUnit.HOURS.toNanos(1))
                    } catch (InterruptedException e) {
                        thrown = e
                    }
                    assert thrown != null
                }
            }
    }

}
