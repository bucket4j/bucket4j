/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j;

import java.io.Serializable;
import java.time.Duration;

/**
 * Specifies the speed of tokens regeneration.
 */
public class Refill implements Serializable {

    private static final long serialVersionUID = 42L;

    private final long periodNanos;
    private final long tokens;

    private Refill(long tokens, Duration period) {
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
    }

    /**
     * Creates refill which regenerates the tokens in greedy manner.
     * This factory method is called "smooth" because of refill created by this method will add tokens to bucket as soon as possible.
     * For example smooth refill "10 tokens per 1 second" will add 1 token per each 100 millisecond,
     * in other words refill will not wait 1 second to regenerate whole bunch of 10 tokens:
     * <pre>
     * <code>Refill.smooth(600, Duration.ofMinutes(1));</code>
     * <code>Refill.smooth(10, Duration.ofSeconds(1));</code>
     * <code>Refill.smooth(1, Duration.ofMillis(100));</code>
     * </pre>
     * The three refills above absolutely equals.
     *
     * @param tokens
     * @param period
     *
     * @return
     */
    public static Refill smooth(long tokens, Duration period) {
        return new Refill(tokens, period);
    }

    public long getPeriodNanos() {
        return periodNanos;
    }

    public long getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        return "Refill{" +
                "periodNanos=" + periodNanos +
                ", tokens=" + tokens +
                '}';
    }

}
