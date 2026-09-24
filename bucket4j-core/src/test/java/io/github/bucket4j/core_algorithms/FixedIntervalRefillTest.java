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
import io.github.bucket4j.Refill;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fixed Interval Refill Specification")
class FixedIntervalRefillTest {

    @Test
    void basicTestOfFixedIntervalRefill() throws Exception {
        Refill refill = Refill.intervally(9, Duration.ofNanos(10));
        Bandwidth bandwidth = Bandwidth.classic(9, refill)
            .withInitialTokens(0);
        TimeMeterMock mockTimer = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .withCustomTimePrecision(mockTimer)
            .addLimit(bandwidth)
            .build();

        assertThat(bucket.getAvailableTokens()).isEqualTo(0);

        mockTimer.addTime(4);
        assertThat(bucket.getAvailableTokens()).isEqualTo(0);

        mockTimer.addTime(6);
        assertThat(bucket.getAvailableTokens()).isEqualTo(9);

        mockTimer.addTime(1);
        assertThat(bucket.getAvailableTokens()).isEqualTo(9);
    }

    @Test
    void complexTestOfFixedIntervalRefill() throws Exception {
        Bandwidth bandwidth1 = Bandwidth.classic(9, Refill.intervally(5, Duration.ofNanos(6)))
            .withInitialTokens(0);
        Bandwidth bandwidth2 = Bandwidth.classic(12, Refill.intervally(4, Duration.ofNanos(5)))
            .withInitialTokens(0);
        TimeMeterMock mockTimer = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .withCustomTimePrecision(mockTimer)
            .addLimit(bandwidth1)
            .addLimit(bandwidth2)
            .build();

        assertThat(bucket.getAvailableTokens()).isEqualTo(0);

        mockTimer.addTime(4); // 4
        assertThat(bucket.getAvailableTokens()).isEqualTo(0);

        mockTimer.addTime(1); // 5
        assertThat(bucket.getAvailableTokens()).isEqualTo(0);

        mockTimer.addTime(1); // 6
        assertThat(bucket.getAvailableTokens()).isEqualTo(4);

        mockTimer.addTime(4); // 10
        assertThat(bucket.getAvailableTokens()).isEqualTo(5);

        mockTimer.addTime(2); // 12
        assertThat(bucket.getAvailableTokens()).isEqualTo(8);

        mockTimer.addTime(3); // 15
        assertThat(bucket.getAvailableTokens()).isEqualTo(9);
    }

    @Test
    void testForRefillTimeEstimationIssue71() throws Exception {
        Refill refill = Refill.intervally(10, Duration.ofMinutes(1));
        Bandwidth bandwidth = Bandwidth.classic(10, refill)
            .withInitialTokens(0);
        TimeMeterMock mockTimer = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .withCustomTimePrecision(mockTimer)
            .addLimit(bandwidth)
            .build();

        io.github.bucket4j.ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        assertThat(probe.isConsumed()).isFalse();
        assertThat(probe.getRemainingTokens()).isEqualTo(0);
        assertThat(probe.getNanosToWaitForRefill()).isEqualTo(TimeUnit.SECONDS.toNanos(60));

        probe = bucket.tryConsumeAndReturnRemaining(10);
        assertThat(probe.isConsumed()).isFalse();
        assertThat(probe.getRemainingTokens()).isEqualTo(0);
        assertThat(probe.getNanosToWaitForRefill()).isEqualTo(TimeUnit.SECONDS.toNanos(60));

        mockTimer.addTime(TimeUnit.SECONDS.toNanos(15));
        probe = bucket.tryConsumeAndReturnRemaining(1);
        assertThat(probe.isConsumed()).isFalse();
        assertThat(probe.getRemainingTokens()).isEqualTo(0);
        assertThat(probe.getNanosToWaitForRefill()).isEqualTo(TimeUnit.SECONDS.toNanos(45));

        probe = bucket.tryConsumeAndReturnRemaining(15);
        assertThat(probe.isConsumed()).isFalse();
        assertThat(probe.getRemainingTokens()).isEqualTo(0);
        assertThat(probe.getNanosToWaitForRefill()).isEqualTo(Long.MAX_VALUE);
    }

}
