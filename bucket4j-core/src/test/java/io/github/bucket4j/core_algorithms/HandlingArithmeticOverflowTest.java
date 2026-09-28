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

package io.github.bucket4j.core_algorithms;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HandlingArithmeticOverflowTest {

    @Test
    void regressionTestForIssue51() throws Exception {
        Bandwidth limit1 = Bandwidth.simple(700000, Duration.ofHours(1));
        Bandwidth limit2 = Bandwidth.simple(14500, Duration.ofMinutes(1));
        Bandwidth limit3 = Bandwidth.simple(300, Duration.ofSeconds(1));
        TimeMeterMock customTimeMeter = new TimeMeterMock(0);
        long twelveHourNanos = 12 * 60 * 60 * 1_000_000_000L;
        Bucket bucket = Bucket.builder()
            .addLimit(limit1)
            .addLimit(limit2)
            .addLimit(limit3)
            .withCustomTimePrecision(customTimeMeter)
            .build();

        // shift time to 12 hours forward
        customTimeMeter.addTime(twelveHourNanos);

        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(300 - 1)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void shouldCheckArithmeticOverflowWhenAddTokensToBucket() throws Exception {
        Bandwidth limit = Bandwidth
            .simple(10, Duration.ofSeconds(1))
            .withInitialTokens(9);
        TimeMeterMock customTimeMeter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .addLimit(limit)
            .withCustomTimePrecision(customTimeMeter)
            .build();

        bucket.addTokens(Long.MAX_VALUE - 1);

        assertThat(bucket.tryConsume(10)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void shouldFirstlyDoRefillByCompletedPeriods() throws Exception {
        Bandwidth limit = Bandwidth.simple(Long.MAX_VALUE / 16, Duration.ofNanos(Long.MAX_VALUE / 8))
            .withInitialTokens(7);
        TimeMeterMock meter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .addLimit(limit)
            .withCustomTimePrecision(meter)
            .build();

        // emulate time shift which equal of 3 refill periods
        meter.addTime(Long.MAX_VALUE / 8 * 3);

        assertThat(bucket.tryConsume(Long.MAX_VALUE / 16)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void shouldCheckArithmeticOverflowWhenRefillingByCompletedPeriods() throws Exception {
        Bandwidth limit = Bandwidth
            .classic(Long.MAX_VALUE - 10, Refill.greedy(1, Duration.ofNanos(1)))
            .withInitialTokens(Long.MAX_VALUE - 13);
        TimeMeterMock meter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .addLimit(limit)
            .withCustomTimePrecision(meter)
            .build();

        // add time shift enough to overflow
        meter.addTime(20);

        assertThat(bucket.tryConsume(Long.MAX_VALUE - 10)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void shouldDownToFloatingPointArithmeticIfNecessaryDuringRefill() throws Exception {
        long capacity = Long.MAX_VALUE / 16L;
        Duration refillPeriod = Duration.ofNanos(Long.MAX_VALUE / 8L);
        Bandwidth limit = Bandwidth
            .builder().capacity(capacity)
            .refillGreedy(capacity, refillPeriod)
            .build();
        TimeMeterMock meter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .addLimit(limit)
            .withCustomTimePrecision(meter)
            .build();

        // emulate time shift which little bit less then one refill period
        meter.addTime(refillPeriod.toNanos() - 1);

        // should down into floating point arithmetic and successfully refill
        assertThat(bucket.tryConsume(capacity / 2)).isTrue();
        assertThat(bucket.getAvailableTokens()).isEqualTo(capacity / 2L + 1);
    }

    @Test
    void shouldCheckArithmeticOverflowWhenRefillingByUncompletedPeriods() throws Exception {
        Bandwidth limit = Bandwidth
            .classic(Long.MAX_VALUE - 10, Refill.greedy(100, Duration.ofNanos(100)))
            .withInitialTokens(Long.MAX_VALUE - 13);
        TimeMeterMock meter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .addLimit(limit)
            .withCustomTimePrecision(meter)
            .build();

        // add time shift enough to overflow
        meter.addTime(50);

        assertThat(bucket.tryConsume(Long.MAX_VALUE - 10)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void shouldCorrectlyCalculateTimeToCloseDeficitWhenHavingDealsWithBigNumber() throws Exception {
        Bandwidth limit = Bandwidth
            .simple(Long.MAX_VALUE / 2, Duration.ofNanos(Long.MAX_VALUE / 2))
            .withInitialTokens(0);
        TimeMeterMock meter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .addLimit(limit)
            .withCustomTimePrecision(meter)
            .build();
        BucketState state = bucket.asVerbose().getAvailableTokens().getState();

        assertThat(state.calculateDelayNanosAfterWillBePossibleToConsume(10, meter.currentTimeNanos(), false)).isEqualTo(10);

        state.consume(1);

        assertThat(state.getAvailableTokens()).isEqualTo(-1);
        assertThat(state.calculateDelayNanosAfterWillBePossibleToConsume(Long.MAX_VALUE, meter.currentTimeNanos(), false)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void shouldDetectOverflowDuringDeficitCalculationForIntervalRefill() throws Exception {
        long refillPeriodNanos = Long.MAX_VALUE / 2;
        long capacity = Long.MAX_VALUE / 2;
        long refillTokens = Long.MAX_VALUE / 4;
        Bandwidth limit = Bandwidth.builder()
            .capacity(capacity)
            .refillIntervally(refillTokens, Duration.ofNanos(refillPeriodNanos))
            .initialTokens(0)
            .build();
        TimeMeterMock meter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .addLimit(limit)
            .withCustomTimePrecision(meter)
            .build();
        BucketState state = bucket.asVerbose().getAvailableTokens().getState();

        assertThat(state.calculateDelayNanosAfterWillBePossibleToConsume(10, meter.currentTimeNanos(), false)).isEqualTo(4611686018427387903L);

        state.consume(1);

        assertThat(state.getAvailableTokens()).isEqualTo(-1);
        assertThat(state.calculateDelayNanosAfterWillBePossibleToConsume(Long.MAX_VALUE, meter.currentTimeNanos(), false)).isEqualTo(Long.MAX_VALUE);
        assertThat(state.calculateDelayNanosAfterWillBePossibleToConsume(Long.MAX_VALUE / 2, meter.currentTimeNanos(), false)).isEqualTo(Long.MAX_VALUE);

        state.addTokens(1);
        meter.addTime(refillPeriodNanos - 10);

        assertThat(state.getAvailableTokens()).isEqualTo(0);
        assertThat(state.calculateDelayNanosAfterWillBePossibleToConsume(Long.MAX_VALUE - 10, meter.currentTimeNanos(), false)).isEqualTo(Long.MAX_VALUE);
    }

}
