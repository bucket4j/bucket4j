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

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ready-made {@link RetryStrategy} implementations for Compare-And-Swap (CAS) based backends.
 *
 * <p>By default, CAS operations retry immediately with no delay between attempts. Under
 * contention on a single hot key, this causes competing clients to collide repeatedly on the
 * same instant, which both wastes round trips and, when combined with {@code maxRetries},
 * can reject requests unfairly: an attempt-count budget can be exhausted in a few
 * milliseconds while the bucket still has tokens available.
 *
 * <p>{@link #exponentialBackoffWithJitter(Duration, Duration, Duration)} addresses this by
 * spacing out retries with randomized (jittered) exponential backoff and bounding retries by
 * elapsed wall-clock time rather than attempt count, so that competing clients desynchronize
 * instead of repeatedly colliding, and any client that has waited for the full budget gets a
 * final, fair answer regardless of how many attempts it happened to make.
 */
public final class RetryStrategies {

    private RetryStrategies() {
    }

    /**
     * Creates a {@link RetryStrategy} that waits for a randomized (jittered) exponentially
     * increasing delay between CAS retry attempts, and stops retrying once the elapsed time
     * since the first attempt reaches {@code budget}.
     *
     * <p>The delay before attempt {@code n} (1-based) is chosen uniformly at random from
     * {@code [min(baseDelay * 2^(n-1), maxDelay) / 2, min(baseDelay * 2^(n-1), maxDelay)]} —
     * "half jitter" — which keeps the expected delay close to the exponential curve while
     * still desynchronizing clients that failed on the same attempt number.
     *
     * @param baseDelay delay used for the first retry attempt. The
     *                   CAS executor re-reads backend state before each attempt, so
     *                   the delay serves to desynchronize competing clients rather
     *                   than to wait for fresh data. Must be positive.
     * @param maxDelay   upper bound on the (pre-jitter) delay; caps added latency under
     *                   sustained contention. Must be greater than or equal to {@code baseDelay}.
     * @param budget     maximum elapsed wall-clock time for scheduling retry delays. Once the
     *                   jittered delay no longer fits within the remaining budget, the strategy
     *                   returns {@link RetryDecision#stop()}. The CAS executor may run one
     *                   final attempt after waking, so total elapsed time may slightly exceed
     *                   the budget by one backend round-trip. Must be positive.
     * @return a retry strategy implementing jittered exponential backoff with a time budget
     */
    public static RetryStrategy exponentialBackoffWithJitter(Duration baseDelay, Duration maxDelay, Duration budget) {
        Objects.requireNonNull(baseDelay, "baseDelay must not be null");
        Objects.requireNonNull(maxDelay, "maxDelay must not be null");
        Objects.requireNonNull(budget, "budget must not be null");
        if (baseDelay.isNegative() || baseDelay.isZero()) {
            throw new IllegalArgumentException("baseDelay must be positive: " + baseDelay);
        }
        if (maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("maxDelay must be >= baseDelay: maxDelay=" + maxDelay + ", baseDelay=" + baseDelay);
        }
        if (budget.isNegative() || budget.isZero()) {
            throw new IllegalArgumentException("budget must be positive: " + budget);
        }

        long baseNanos = baseDelay.toNanos();
        long maxNanos = maxDelay.toNanos();
        long budgetNanos = budget.toNanos();

        return metadata -> {
            if (metadata.getElapsedTimeNanos() >= budgetNanos) {
                return RetryDecision.stop();
            }
            int shift = Math.max(metadata.getAttemptNumber() - 1, 0);
            long expNanos;
            if (shift >= 63 || baseNanos > (maxNanos >> shift)) {
                // baseNanos << shift would meet/exceed maxNanos, or would overflow a long; either way, saturate.
                expNanos = maxNanos;
            } else {
                expNanos = baseNanos << shift;
            }
            long lower = expNanos / 2;
            long range = expNanos - lower + 1;
            long jitteredNanos = expNanos <= 1 ? expNanos : lower + ThreadLocalRandom.current().nextLong(range);
            long remainingNanos = budgetNanos - metadata.getElapsedTimeNanos();
            if (jitteredNanos >= remainingNanos) {
                return RetryDecision.stop();
            }
            return RetryDecision.retryAfter(Duration.ofNanos(jitteredNanos));
        };
    }
}
