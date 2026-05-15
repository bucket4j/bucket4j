/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2026 Ivan Vaskevych
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

/**
 * Describes how the CAS executor should proceed after a failed compare-and-swap attempt.
 */
public final class RetryDecision {

    private static final RetryDecision STOP = new RetryDecision(false, Duration.ZERO);
    private static final RetryDecision RETRY_IMMEDIATELY = new RetryDecision(true, Duration.ZERO);

    private final boolean retry;
    private final Duration delay;

    private RetryDecision(boolean retry, Duration delay) {
        this.retry = retry;
        this.delay = Objects.requireNonNull(delay);
    }

    public static RetryDecision stop() {
        return STOP;
    }

    public static RetryDecision retryImmediately() {
        return RETRY_IMMEDIATELY;
    }

    public static RetryDecision retryAfter(Duration delay) {
        Objects.requireNonNull(delay);
        if (delay.isNegative()) {
            throw new IllegalArgumentException("Retry delay can not be negative: " + delay);
        }
        if (delay.isZero()) {
            return RETRY_IMMEDIATELY;
        }
        return new RetryDecision(true, delay);
    }

    public boolean shouldRetry() {
        return retry;
    }

    public Duration getDelay() {
        return delay;
    }
}
