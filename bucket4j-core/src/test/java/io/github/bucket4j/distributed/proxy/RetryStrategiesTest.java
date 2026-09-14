/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.distributed.proxy.RetryStrategy.RetryMetadata;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RetryStrategiesTest {

    private static final Duration BASE = Duration.ofMillis(1);
    private static final Duration MAX = Duration.ofMillis(20);
    private static final Duration BUDGET = Duration.ofMillis(150);

    private static RetryMetadata metadata(int attemptNumber, long elapsedNanos) {
        long start = 0L;
        return new RetryMetadata(attemptNumber, "key", start, start + elapsedNanos);
    }

    @Test
    public void rejectsNonPositiveBaseDelay() {
        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategies.exponentialBackoffWithJitter(Duration.ZERO, MAX, BUDGET));
        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategies.exponentialBackoffWithJitter(Duration.ofMillis(-1), MAX, BUDGET));
    }

    @Test
    public void rejectsMaxDelayLessThanBaseDelay() {
        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategies.exponentialBackoffWithJitter(Duration.ofMillis(10), Duration.ofMillis(1), BUDGET));
    }

    @Test
    public void rejectsNonPositiveBudget() {
        assertThrows(IllegalArgumentException.class, () ->
                RetryStrategies.exponentialBackoffWithJitter(BASE, MAX, Duration.ZERO));
    }

    @Test
    public void stopsOnceBudgetExhausted() {
        RetryStrategy strategy = RetryStrategies.exponentialBackoffWithJitter(BASE, MAX, BUDGET);
        RetryDecision decision = strategy.shouldRetry(metadata(1, BUDGET.toNanos()));
        assertFalse(decision.shouldRetry());
    }

    @Test
    public void retriesWithDelayWithinBudget() {
        RetryStrategy strategy = RetryStrategies.exponentialBackoffWithJitter(BASE, MAX, BUDGET);
        RetryDecision decision = strategy.shouldRetry(metadata(1, 0));
        assertTrue(decision.shouldRetry());
        assertTrue(decision.getDelay().toNanos() > 0);
        assertTrue(decision.getDelay().compareTo(BASE) <= 0);
    }

    @Test
    public void delayNeverExceedsMaxDelay() {
        RetryStrategy strategy = RetryStrategies.exponentialBackoffWithJitter(BASE, MAX, BUDGET);

        for (int attempt = 1; attempt <= 10; attempt++) {
            RetryDecision decision = strategy.shouldRetry(metadata(attempt, 0));
            assertTrue(decision.shouldRetry());
            long delayNanos = decision.getDelay().toNanos();
            assertTrue(delayNanos > 0, "delay should be positive");
            assertTrue(delayNanos <= MAX.toNanos(), "delay should never exceed maxDelay");
        }
    }

    @Test
    public void delaySaturatesAtMaxDelayForLargeAttemptNumbers() {
        // base=1ms doubling per attempt saturates max=20ms well before attempt 10 (2^5ms=32ms > 20ms)
        RetryStrategy strategy = RetryStrategies.exponentialBackoffWithJitter(BASE, MAX, BUDGET);
        for (int attempt : new int[] {10, 100, Integer.MAX_VALUE}) {
            RetryDecision decision = strategy.shouldRetry(metadata(attempt, 0));
            assertTrue(decision.shouldRetry());
            long delayNanos = decision.getDelay().toNanos();
            assertTrue(delayNanos >= MAX.toNanos() / 2, "delay should be jittered around maxDelay once saturated");
            assertTrue(delayNanos <= MAX.toNanos());
        }
    }

    @Test
    public void handlesMaxValueMaxDelayWithoutOverflow() {
        Duration maxValDelay = Duration.ofNanos(Long.MAX_VALUE);
        RetryStrategy strategy = RetryStrategies.exponentialBackoffWithJitter(BASE, maxValDelay, Duration.ofNanos(Long.MAX_VALUE));
        RetryDecision decision = strategy.shouldRetry(metadata(63, 0));
        assertTrue(decision.shouldRetry());
        long delayNanos = decision.getDelay().toNanos();
        assertTrue(delayNanos >= maxValDelay.toNanos() / 2);
        assertTrue(delayNanos <= maxValDelay.toNanos());
    }

    @Test
    public void stopsWhenJitteredDelayExceedsRemainingBudget() {
        RetryStrategy strategy = RetryStrategies.exponentialBackoffWithJitter(BASE, MAX, BUDGET);
        long remainingNanos = BASE.toNanos() / 2;
        long elapsedNanos = BUDGET.toNanos() - remainingNanos;
        RetryDecision decision = strategy.shouldRetry(metadata(1, elapsedNanos));
        assertFalse(decision.shouldRetry(),
                "should stop when remaining budget is smaller than the jittered delay");
    }
}
