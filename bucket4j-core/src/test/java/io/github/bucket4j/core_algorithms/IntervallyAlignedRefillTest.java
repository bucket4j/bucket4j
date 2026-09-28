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
import io.github.bucket4j.MathType;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Intervally Aligned Refill Specification")
class IntervallyAlignedRefillTest {

    private record InitialTokensCase(
        long currentTimeMillis,
        long firstRefillTimeMillis,
        long capacity,
        long refillTokens,
        long refillPeriodMillis,
        long requiredInitialTokens,
        MathType mathType
    ) {

        @Override
        public String toString() {
            return "currentTimeMillis=" + currentTimeMillis + ", firstRefillTimeMillis=" + firstRefillTimeMillis;
        }
    }

    private record AlignedRefillCase(MathType mathType) {

        @Override
        public String toString() {
            return "mathType=" + mathType;
        }
    }

    static Stream<InitialTokensCase> initialTokenCalculation() {
        return Stream.of(
            new InitialTokensCase(60_000, 60_000, 400, 400, 60_000, 400, MathType.INTEGER_64_BITS),
            new InitialTokensCase(20_000, 60_000, 400, 400, 60_000, 266, MathType.INTEGER_64_BITS),
            new InitialTokensCase(20_000, 60_000, 400, 300, 60_000, 300, MathType.INTEGER_64_BITS),
            new InitialTokensCase(20_000, 60_000, 400, 200, 60_000, 333, MathType.INTEGER_64_BITS),
            new InitialTokensCase(20_000, 60_000, 400, 100, 60_000, 366, MathType.INTEGER_64_BITS),
            new InitialTokensCase(60_000, 60_000, 400, 100, 60_000, 400, MathType.INTEGER_64_BITS),
            new InitialTokensCase(60_001, 60_000, 400, 100, 60_000, 400, MathType.INTEGER_64_BITS),
            new InitialTokensCase(20_000, 180_000, 400, 100, 60_000, 400, MathType.INTEGER_64_BITS),
            new InitialTokensCase(20_000, 60_000, 100, 400, 60_000, 100, MathType.INTEGER_64_BITS),
            new InitialTokensCase(59_000, 60_000, 100, 400, 60_000, 6, MathType.INTEGER_64_BITS)
        );
    }

    static Stream<AlignedRefillCase> mathTypes() {
        return Stream.of(MathType.values()).map(AlignedRefillCase::new);
    }

    @ParameterizedTest
    @MethodSource("initialTokenCalculation")
    void calculatesInitialTokensCorrectly(InitialTokensCase testCase) throws Exception {
        Instant firstRefillTime = Instant.ofEpochMilli(testCase.firstRefillTimeMillis());
        Duration refillPeriod = Duration.ofMillis(testCase.refillPeriodMillis());
        Refill refill = Refill.intervallyAligned(testCase.refillTokens(), refillPeriod, firstRefillTime, true);
        Bandwidth bandwidth = Bandwidth.classic(testCase.capacity(), refill);
        TimeMeterMock mockTimer = new TimeMeterMock(testCase.currentTimeMillis() * 1_000_000);

        Bucket bucket = Bucket.builder()
            .withCustomTimePrecision(mockTimer)
            .addLimit(bandwidth)
            .build();

        assertThat(bucket.getAvailableTokens()).isEqualTo(testCase.requiredInitialTokens());
    }

    @ParameterizedTest
    @MethodSource("mathTypes")
    void complexSpecWhenUseAdaptiveInitialTokensFalse(AlignedRefillCase testCase) throws Exception {
        // having the refill 200 tokens/1 minute, capacity is 400,
        // 20 seconds past from beginning of current minute,
        // first refill planned to next minute
        Instant firstRefillTime = Instant.ofEpochMilli(TimeUnit.SECONDS.toMillis(120));
        Refill refill = Refill.intervallyAligned(200, Duration.ofMinutes(1), firstRefillTime, false);
        Bandwidth bandwidth = Bandwidth.classic(400, refill);
        TimeMeterMock mockTimer = new TimeMeterMock();
        mockTimer.setCurrentTimeSeconds(80);

        Bucket bucket = Bucket.builder()
            .withCustomTimePrecision(mockTimer)
            .addLimit(bandwidth)
            .build();

        // initialTokens == capacity because useAdaptiveInitialTokens == false
        assertThat(bucket.getAvailableTokens()).isEqualTo(400);

        // when all tokens consumed and 10 seconds elapsed
        bucket.tryConsumeAsMuchAsPossible();
        mockTimer.addSeconds(10);

        // available tokens should be zero because refill is not greedy
        assertThat(bucket.getAvailableTokens()).isEqualTo(0);
        // bucket should report that it is need to wait 30 seconds before first token will be available
        assertThat(bucket.tryConsumeAndReturnRemaining(1).getNanosToWaitForRefill()).isEqualTo(TimeUnit.SECONDS.toNanos(30));

        // when yet another 30 seconds elapsed, 200 tokens should be added to bucket
        mockTimer.addSeconds(30);
        assertThat(bucket.getAvailableTokens()).isEqualTo(200);

        // when yet another 45 seconds elapsed, nothing should be added to bucket
        mockTimer.addSeconds(45);
        assertThat(bucket.getAvailableTokens()).isEqualTo(200);

        // when yet another 15 seconds elapsed, 200 tokens should be added to bucket
        mockTimer.addSeconds(15);
        assertThat(bucket.getAvailableTokens()).isEqualTo(400);

        // when yet another 60 seconds elapsed, nothing should be added to bucket because max capacity already reached
        mockTimer.addSeconds(60);
        assertThat(bucket.getAvailableTokens()).isEqualTo(400);

        // when all tokens consumed
        bucket.tryConsumeAsMuchAsPossible();

        // bucket should report that 3 minute required to wait in order to consume 401 tokens
        assertThat(bucket.consumeIgnoringRateLimits(401)).isEqualTo(TimeUnit.MINUTES.toNanos(3));
    }

    @ParameterizedTest
    @MethodSource("mathTypes")
    void complexSpecWhenUseAdaptiveInitialTokensTrue(AlignedRefillCase testCase) throws Exception {
        // having the refill 200 tokens/1 minute, capacity is 400,
        // 20 seconds past from beginning of current minute,
        // first refill planned to next minute
        Instant firstRefillTime = Instant.ofEpochMilli(TimeUnit.SECONDS.toMillis(120));
        Refill refill = Refill.intervallyAligned(200, Duration.ofMinutes(1), firstRefillTime, true);
        Bandwidth bandwidth = Bandwidth.classic(400, refill);
        TimeMeterMock mockTimer = new TimeMeterMock();
        mockTimer.setCurrentTimeSeconds(80);

        Bucket bucket = Bucket.builder()
            .withCustomTimePrecision(mockTimer)
            .addLimit(bandwidth)
            .build();

        // initialTokens == 333 because useAdaptiveInitialTokens == true
        assertThat(bucket.getAvailableTokens()).isEqualTo(333);

        // when all tokens consumed and 10 seconds elapsed
        bucket.tryConsumeAsMuchAsPossible();
        mockTimer.addSeconds(10);

        // available tokens should be zero because refill is not greedy
        assertThat(bucket.getAvailableTokens()).isEqualTo(0);
        // bucket should report that it is need to wait 30 seconds before first token will be available
        assertThat(bucket.tryConsumeAndReturnRemaining(1).getNanosToWaitForRefill()).isEqualTo(TimeUnit.SECONDS.toNanos(30));

        // when yet another 30 seconds elapsed, 200 tokens should be added to bucket
        mockTimer.addSeconds(30);
        assertThat(bucket.getAvailableTokens()).isEqualTo(200);

        // when yet another 45 seconds elapsed, nothing should be added to bucket
        mockTimer.addSeconds(45);
        assertThat(bucket.getAvailableTokens()).isEqualTo(200);

        // when yet another 15 seconds elapsed, 200 tokens should be added to bucket
        mockTimer.addSeconds(15);
        assertThat(bucket.getAvailableTokens()).isEqualTo(400);

        // when yet another 60 seconds elapsed, nothing should be added to bucket because max capacity already reached
        mockTimer.addSeconds(60);
        assertThat(bucket.getAvailableTokens()).isEqualTo(400);

        // when all tokens consumed
        bucket.tryConsumeAsMuchAsPossible();

        // bucket should report that 3 minute required to wait in order to consume 401 tokens
        assertThat(bucket.consumeIgnoringRateLimits(401)).isEqualTo(TimeUnit.MINUTES.toNanos(3));
    }

    @ParameterizedTest
    @MethodSource("mathTypes")
    void boundaryOfIntervalIsNotMissedDuringLongTimeOfInactivity(AlignedRefillCase testCase) throws Exception {
        TimeMeterMock timeMeter = new TimeMeterMock(TimeUnit.MILLISECONDS.toNanos(103));

        Refill refill = Refill.intervallyAligned(400, Duration.ofMillis(100), Instant.ofEpochMilli(200), false);
        LocalBucket bucket = Bucket.builder()
            .withCustomTimePrecision(timeMeter)
            .addLimit(Bandwidth.classic(400, refill))
            .build();

        bucket.tryConsumeAsMuchAsPossible();

        // when bucket was inactive for significant time and consumption resumed something in the middle of interval
        timeMeter.setCurrentTimeMillis(32849);

        // bucket should be refilled to max capacity
        assertThat(bucket.getAvailableTokens()).isEqualTo(400);

        // original boundary of interval should not be missed, next refill date should be chosen based on original boundary instead of current time
        assertThat(bucket.tryConsumeAsMuchAsPossible()).isEqualTo(400);
        assertThat(bucket.getAvailableTokens()).isEqualTo(0);
        timeMeter.addMillis(51);
        assertThat(bucket.getAvailableTokens()).isEqualTo(400);
    }

}
