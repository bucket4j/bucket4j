/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
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

package io.github.bucket4j;

import java.time.Duration;

/**
 * Specifies the speed of tokens regeneration.
 * <p>
 * This class is deprecated, you should use {@link Bandwidth#builder()}
 * @deprecated
 */
@Deprecated
public class Refill {

    final long periodNanos;
    final long tokens;
    final boolean refillIntervally;
    final long timeOfFirstRefillMillis;
    final boolean useAdaptiveInitialTokens;

    private Refill(long tokens, Duration period, boolean refillIntervally, long timeOfFirstRefillMillis, boolean useAdaptiveInitialTokens) {
        if (period == null) {
            throw BucketExceptions.nullRefillPeriod();
        }
        if (tokens <= 0) {
            throw BucketExceptions.nonPositivePeriodTokens(tokens);
        }
        this.periodNanos = period.toNanos();
        if (periodNanos <= 0) {
            throw BucketExceptions.nonPositivePeriod(periodNanos);
        }
        if (tokens > periodNanos) {
            throw BucketExceptions.tooHighRefillRate(periodNanos, tokens);
        }

        this.tokens = tokens;
        this.refillIntervally = refillIntervally;
        this.timeOfFirstRefillMillis = timeOfFirstRefillMillis;
        this.useAdaptiveInitialTokens = useAdaptiveInitialTokens;
    }

    @Override
    public String toString() {
        return "Refill{" + "periodNanos=" + periodNanos +
            ", tokens=" + tokens +
            ", refillIntervally=" + refillIntervally +
            ", timeOfFirstRefillMillis=" + timeOfFirstRefillMillis +
            ", useAdaptiveInitialTokens=" + useAdaptiveInitialTokens +
            '}';
    }

}
