package com.github.bucket4j;

import java.io.Serializable;
import java.time.Duration;

import static com.github.bucket4j.BucketExceptions.*;

/**
 * Specifies the speed of regeneration for consumed tokens.
 */
public class Refill implements Serializable {

    private final long periodNanos;
    private final long tokens;

    private Refill(long tokens, Duration period) {
        if (tokens <= 0) {
            throw nonPositivePeriodTokens(tokens);
        }
        this.tokens = tokens;

        if (period == null) {
            throw nullPeriod();
        }
        this.periodNanos = period.toNanos();
        if (periodNanos <= 0) {
            throw nonPositivePeriod(periodNanos);
        }
    }

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
