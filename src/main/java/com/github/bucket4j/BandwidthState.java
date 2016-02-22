package com.github.bucket4j;

import java.io.Serializable;

public class BandwidthState implements Serializable {

    private long currentSize;
    private long roundingError;

    private BandwidthState(long currentSize, long roundingError) {
        this.currentSize = currentSize;
        this.roundingError = roundingError;
    }

    public static BandwidthState initialState(long currentSize) {
        return new BandwidthState(currentSize, 0L);
    }

    public BandwidthState clone() {
        return new BandwidthState(currentSize, roundingError);
    }

    public void copyStateFrom(BandwidthState sourceState) {
        currentSize = sourceState.currentSize;
        roundingError = sourceState.roundingError;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public long getRoundingError() {
        return roundingError;
    }

    public void setCurrentSize(long currentSize) {
        this.currentSize = currentSize;
    }

    public void setRoundingError(long roundingError) {
        this.roundingError = roundingError;
    }

    @Override
    public String toString() {
        return "BandwidthState{" +
                "currentSize=" + currentSize +
                ", roundingError=" + roundingError +
                '}';
    }

}
