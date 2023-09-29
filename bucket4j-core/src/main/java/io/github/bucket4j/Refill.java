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
import java.time.Instant;

import io.github.bucket4j.BandwidthBuilder.BandwidthBuilderRefillStage;

/**
 * Specifies the speed of tokens regeneration.
 *
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

    @Deprecated
    public static Refill of(long tokens, Duration period) {
        return greedy(tokens, period);
    }

    @Deprecated
    public static Refill smooth(long tokens, Duration period) {
        return greedy(tokens, period);
    }

    /**
     * This method is deprecated, you should use {@link BandwidthBuilderRefillStage#refillGreedy(long, Duration)}
     *
     * @deprecated
     */
    @Deprecated
    public static Refill greedy(long tokens, Duration period) {
        return new Refill(tokens, period, false, Bandwidth.UNSPECIFIED_TIME_OF_FIRST_REFILL, false);
    }

    /**
     * This method is deprecated, you should use {@link BandwidthBuilderRefillStage#refillIntervally(long, Duration)}
     *
     * @deprecated
     */
    @Deprecated
    public static Refill intervally(long tokens, Duration period) {
        return new Refill(tokens, period, true, Bandwidth.UNSPECIFIED_TIME_OF_FIRST_REFILL, false);
    }


    /**
     * This method is deprecated, you should use {@link BandwidthBuilderRefillStage#refillIntervallyAligned(long, Duration, Instant)} or 
     * {@link BandwidthBuilderRefillStage#refillIntervallyAlignedWithAdaptiveInitialTokens(long, Duration, Instant)}
     *
     * @deprecated
     */
    @Deprecated
    public static Refill intervallyAligned(long tokens, Duration period, Instant timeOfFirstRefill, boolean useAdaptiveInitialTokens) {
        long timeOfFirstRefillMillis = timeOfFirstRefill.toEpochMilli();
        if (timeOfFirstRefillMillis < 0) {
            throw BucketExceptions.nonPositiveTimeOfFirstRefill(timeOfFirstRefill);
        }
        return new Refill(tokens, period, true, timeOfFirstRefillMillis, useAdaptiveInitialTokens);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Refill{");
        sb.append("periodNanos=").append(periodNanos);
        sb.append(", tokens=").append(tokens);
        sb.append(", refillIntervally=").append(refillIntervally);
        sb.append(", timeOfFirstRefillMillis=").append(timeOfFirstRefillMillis);
        sb.append(", useAdaptiveInitialTokens=").append(useAdaptiveInitialTokens);
        sb.append('}');
        return sb.toString();
    }

}
