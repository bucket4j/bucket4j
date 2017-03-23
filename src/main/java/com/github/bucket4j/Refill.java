package com.github.bucket4j;

import java.io.Serializable;
import java.time.Duration;

import static com.github.bucket4j.BucketExceptions.*;

/**
 * Specifies the speed of tokens regeneration.
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

    long getPeriodNanos() {
        return periodNanos;
    }

    long getTokens() {
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
