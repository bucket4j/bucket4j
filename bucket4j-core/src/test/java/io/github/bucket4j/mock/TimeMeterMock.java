
package io.github.bucket4j.mock;

import io.github.bucket4j.TimeMeter;


public class TimeMeterMock implements TimeMeter {

    private long currentTimeNanos;

    public TimeMeterMock() {
        currentTimeNanos = 0;
    }

    public TimeMeterMock(long currentTimeNanos) {
        this.currentTimeNanos = currentTimeNanos;
    }

    public void addTime(long nanos) {
        currentTimeNanos += nanos;
    }

    public void setCurrentTimeNanos(long currentTimeNanos) {
        this.currentTimeNanos = currentTimeNanos;
    }

    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeNanos = currentTimeMillis * 1_000_000;
    }

    public void addMillis(long millis) {
        currentTimeNanos += millis * 1_000_000;
    }

    public void setCurrentTimeSeconds(long currentTimeSeconds) {
        this.currentTimeNanos = currentTimeSeconds * 1_000_000_000;
    }

    public void addSeconds(long seconds) {
        currentTimeNanos += seconds * 1_000_000_000;
    }

    @Override
    public long currentTimeNanos() {
        return currentTimeNanos;
    }

    @Override
    public boolean isWallClockBased() {
        return true;
    }

}
