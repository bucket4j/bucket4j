package com.github.bucket4j;

import java.io.Serializable;

public class Refill implements Serializable {

    private final long periodNanos;
    private final long tokens;

    public Refill(long periodNanos, long tokens) {
        this.periodNanos = periodNanos;
        this.tokens = tokens;
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
