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

package io.github.bucket4j;

import io.github.bucket4j.local.LocalBucketBuilder;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/**
 * Specifies the speed of tokens regeneration.
 */
public class Refill implements Serializable {

    static final long UNSPECIFIED_TIME_OF_FIRST_REFILL = Long.MIN_VALUE;

    private static final long serialVersionUID = 42L;

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
     * Creates the {@link Refill} that does refill of tokens in greedy manner,
     * it will try to add the tokens to bucket as soon as possible.
     * For example "of" refill "10 tokens per 1 second" will add 1 token per each 100 millisecond,
     * in other words refill will not wait 1 second to regenerate whole bunch of 10 tokens.
     *
     * <p>
     * The three refills bellow do refill of tokens with same speed:
     * <pre>
     *      <code>Refill.greedy(600, Duration.ofMinutes(1));</code>
     *      <code>Refill.greedy(10, Duration.ofSeconds(1));</code>
     *      <code>Refill.greedy(1, Duration.ofMillis(100));</code>
     * </pre>
     *
     * <p>
     * If greediness is undesired then you can specify the fixed interval refill via {@link #intervally(long, Duration)}
     *
     * @param tokens amount of tokens
     * @param period the period within {@code tokens} will be fully regenerated
     *
     * @return the {@link Refill} that does refill of tokens in of manner
     */
    public static Refill greedy(long tokens, Duration period) {
        return new Refill(tokens, period, false, UNSPECIFIED_TIME_OF_FIRST_REFILL, false);
    }

    /**
     * Creates the {@link Refill} that does refill of tokens in intervally manner.
     * "Intervally" in opposite to "greedy"  will wait until whole {@code period} will be elapsed before regenerate {@code tokens}
     *
     * @param tokens amount of tokens
     * @param period the period within {@code tokens} will be fully regenerated
     *
     * @return the {@link Refill} that does refill of tokens in intervally manner
     */
    public static Refill intervally(long tokens, Duration period) {
        return new Refill(tokens, period, true, UNSPECIFIED_TIME_OF_FIRST_REFILL, false);
    }


    /**
     * Creates the {@link Refill} that does refill of tokens in intervally manner.
     * "Intervally" in opposite to "greedy"  will wait until whole {@code period} will be elapsed before regenerate {@code tokens}.
     * <br>
     * In additional to {@link #intervally(long, Duration)}</p> it is possible to specify the time when first refill should happen via {@code timeOfFirstRefill}.
     * This option can be used to configure clear interval boundary i.e. start of second, minute, hour, day.
     *
     * <p>
 *     <strong>Special notes about useAdaptiveInitialTokens:</strong>
     * <br>If {@code useAdaptiveInitialTokens == true} and timeOfFirstRefill is a moment in the future, then initial amount of tokens in the bandwidth will be calculated by following formula:
     * <br><pre>{@code
     *     Math.min(capacity, Math.max(0, bandwidthCapacity - refillTokens) + (timeOfFirstRefillMillis - nowMillis)/refillPeriod * refillTokens)
     * }</pre>
     * <br>Bellow the list of examples of how does this formula can be applied:
     * <pre>
     * {@code
     *         // imagine that wall clock is 16:20, the first refill will happen at 17:00
     *         // first refill will happen in the beginning of next hour
     *         Instant firstRefillTime = ZonedDateTime.now()
     *                 .truncatedTo(ChronoUnit.HOURS)
     *                 .plus(1, ChronoUnit.HOURS)
     *                 .toInstant();
     *
     *        // initial tokens 266 will be
     *        Bandwidth.classic(400, Refill.intervallyAligned(400, Duration.ofHours(1), firstRefillTime, true));
     *        // calculated by formula min(400, max(0, 400 - 400) +  40/60*400) = min(400, 0 + 266) = 266
     *
     *        // initial tokens will be 300
     *        Bandwidth.classic(400, Refill.intervallyAligned(300, Duration.ofHours(1), firstRefillTime, true));
     *        // calculated by formula min(400, max(0, 400 - 300) +  40/60*300) = min(400, 100 + 200) = 300
     *
     *        // initial tokens will be 333
     *        Bandwidth.classic(400, Refill.intervallyAligned(200, Duration.ofHours(1), firstRefillTime, true));
     *        // calculated by formula min(400, max(0, 400 - 200) +  40/60*200) = min(400, 200 + 133) = 333
     *
     *        // initial tokens will be 366
     *        Bandwidth.classic(400, Refill.intervallyAligned(100, Duration.ofHours(1), firstRefillTime, true));
     *        // calculated by formula min(400, max(0, 400 - 100) +  40/60*100) = min(400, 300 + 66) = 366
     * }</pre>
     *
     * <ul>
     *     <strong>Restrictions:</strong>
     *     <ul>
     *         <li>
     *             If {@code useAdaptiveInitialTokens} is {@code true} then any attempt to explicitly specify initial amount of tokens via {@link Bandwidth#withInitialTokens(long)}
     *             will fail with exception, because it is impossible at the same time to specify tokens in explicitly and adaptively manners.
     *         </li>
     *         <li>
     *             It is impossible to use this method together with nanoTime based clock {@link LocalBucketBuilder#withNanosecondPrecision()}, because we need in {@link System#currentTimeMillis()} based clock
     *             in order to properly measure the distance from {@code timeOfFirstRefill}
     *         </li>
     *     </ul>
     * </ul>
     *
     * @param tokens amount of tokens
     * @param period the period within {@code tokens} will be fully regenerated
     * @param timeOfFirstRefill the time of first refill, typically it should be a moment in the future
     * @param useAdaptiveInitialTokens if {@code true} then initialTokens may be reduced
     *
     * @return the {@link Refill} that does refill of tokens in intervally manner
     */
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
