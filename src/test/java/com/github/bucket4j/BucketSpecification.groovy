/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j

import com.github.bucket4j.mock.BucketType
import com.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.TimeUnit

class BucketSpecification extends Specification {

    @Unroll
    def "#n Should return #requiredResult when trying to consume single token from Bucket #builder"(
            int n, boolean requiredResult, BucketBuilder builder) {
        expect:
            for (Bucket bucket : BucketType.createBuckets(builder)) {
                assert bucket.tryConsumeSingleToken() == requiredResult
            }
        where:
            n | requiredResult |  builder
            1 |     false      |  BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 0, Duration.ofMinutes(100))
            2 |      true      |  BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofMinutes(100))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consume #toConsume tokens from Bucket #builder"(
            int n, boolean requiredResult, long toConsume, BucketBuilder builder) {
        expect:
            for (Bucket bucket : BucketType.createBuckets(builder)) {
                assert bucket.tryConsume(toConsume) == requiredResult
            }
        where:
            n | requiredResult | toConsume | builder
            1 |     false      |     1     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 0, Duration.ofMinutes(100))
            2 |      true      |     1     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofMinutes(100))
    }

    @Unroll
    def "#n Should return #requiredResult when consumeAsMuchAsPossible tokens from Bucket #builder"(
            int n, long requiredResult, BucketBuilder builder) {
        expect:
            for (Bucket bucket : BucketType.createBuckets(builder)) {
                assert bucket.consumeAsMuchAsPossible() == requiredResult
            }
        where:
            n | requiredResult | builder
            1 |        0       | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 0, Duration.ofMinutes(100))
            2 |        1       | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofMinutes(100))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consumeAsMuchAsPossible with limit #limit tokens from Bucket #builder"(
            int n, long requiredResult, long limit, BucketBuilder builder) {
        expect:
            for (Bucket bucket : BucketType.createBuckets(builder)) {
                assert bucket.consumeAsMuchAsPossible(limit) == requiredResult
            }
        where:
            n | requiredResult |   limit   | builder
            1 |       4        |     5     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 4, Duration.ofMinutes(100))
            2 |       5        |     5     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 5, Duration.ofMinutes(100))
            3 |       5        |     5     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 6, Duration.ofMinutes(100))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep when consuming single token from Bucket #builder"(
            int n, long requiredSleep, BucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                Bucket bucket = type.createBucket(builder)
                TimeMeterMock meter = bucket.getConfiguration().getTimeMeter()
                bucket.consumeSingleToken()
                assert meter.sleeped == requiredSleep
                meter.reset();
            }
        where:
            n | requiredSleep |  builder
            1 |      10       |  BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 0, Duration.ofNanos(100))
            2 |       0       |  BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofNanos(100))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep when trying to consuming #toConsume tokens from Bucket #builder"(
            int n, long requiredSleep, long toConsume, BucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                Bucket bucket = type.createBucket(builder)
                TimeMeterMock meter = bucket.getConfiguration().getTimeMeter()
                bucket.consume(toConsume)
                assert meter.sleeped == requiredSleep
                meter.reset();
            }
        where:
            n | requiredSleep | toConsume | builder
            1 |      10       |     1     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 0, Duration.ofNanos(100))
            2 |       0       |     1     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofNanos(100))
            3 |       0       |  1000     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofNanos(100))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to consume single token with limit #sleepLimit from Bucket #builder"(
            int n, long requiredSleep, boolean requiredResult, long sleepLimit, BucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                Bucket bucket = type.createBucket(builder)
                TimeMeterMock meter = bucket.getConfiguration().getTimeMeter()
                bucket.tryConsumeSingleToken(sleepLimit) == requiredResult
                assert meter.sleeped == requiredSleep
                meter.reset();
            }
        where:
            n | requiredSleep | requiredResult | sleepLimit | builder
            1 |      10       |     true       |     11     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 0, Duration.ofNanos(100))
            2 |       0       |     true       |     11     | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofNanos(100))
    }

    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    @Unroll
    def "#n Should sleep #requiredSleep and return #requiredResult when trying to consume #toConsume tokens with limit #sleepLimit from Bucket #builder"(
            int n, long requiredSleep, boolean requiredResult, long toConsume, long sleepLimit, long timeIncrementAfterEachSleep, BucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                Bucket bucket = type.createBucket(builder)
                TimeMeterMock meter = bucket.getConfiguration().getTimeMeter()
                meter.setIncrementAfterEachSleep(timeIncrementAfterEachSleep)
                bucket.tryConsume(toConsume, sleepLimit)
                assert meter.sleeped == requiredSleep
                meter.reset();
            }
        where:
            n | requiredSleep | requiredResult | toConsume | sleepLimit | timeIncrementAfterEachSleep | builder
            1 |      10       |     true       |     1     |     11     |             0               | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 0, Duration.ofNanos(100))
            2 |      10       |     false      |     1     |     11     |            500              | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 0, Duration.ofNanos(100))
            3 |       0       |     true       |     1     |     11     |             0               | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofNanos(100))
            4 |       0       |     false      |   1000    |     11     |             0               | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofNanos(100))
            5 |       0       |     false      |     5     |     11     |             0               | BucketBuilder.forCustomTimePrecision(new TimeMeterMock(0)).withLimitedBandwidth(10, 1, Duration.ofNanos(100))
    }

}
