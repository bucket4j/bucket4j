package io.github.bucket4j.grid;

import java.io.Serializable;

public abstract class AbstractGridCommand<T extends Serializable> implements GridCommand<T> {

    private long currentTimeNanos;

    public void setCurrentTimeNanos(long currentTimeNanos) {
        this.currentTimeNanos = currentTimeNanos;
    }

    public long getCurrentTimeNanos() {
        return currentTimeNanos;
    }

}
