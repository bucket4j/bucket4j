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

import java.time.Duration;

/**
 * Specifies the speed of tokens regeneration.
 */
public class Refill {

    final long periodNanos;
    final long tokens;
    final boolean refillIntervally;

    private Refill(long tokens, Duration period, boolean refillIntervally) {
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
        return new Refill(tokens, period, false);
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
        return new Refill(tokens, period, true);
    }

    @Override
    public String toString() {
        return "Refill{" +
                "periodNanos=" + periodNanos +
                ", tokens=" + tokens +
                '}';
    }

}
