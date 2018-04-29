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

    final long periodNanos;
    final long tokens;

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
     * Creates the new instance of {@link Refill} that specifies the speed of tokens regeneration
     *
     * @param tokens amount of tokens
     * @param period the period within {@code tokens} will be fully regenerated
     *
     * @return the specification for speed of tokens regeneration.
     */
    public static Refill of(long tokens, Duration period) {
        return new Refill(tokens, period);
    }

    @Override
    public String toString() {
        return "Refill{" +
                "periodNanos=" + periodNanos +
                ", tokens=" + tokens +
                '}';
    }

}
