package ru.vbukhtoyarov.concurrency.tokenbucket;

import java.util.concurrent.TimeUnit;

public class Bandwidth {

    private final long capacity;
    private final long periodInNanos;
    private final RefillStrategy refillStrategy;
    private final WaitingStrategy waitingStrategy;

    /**
     * Creates a Bandwidth.
     *
     * @param capacity
     * @param period
     * @param unit
     * @param refillStrategy
     * @param waitingStrategy
     */
    public Bandwidth(long capacity, long period, TimeUnit unit, RefillStrategy refillStrategy, WaitingStrategy waitingStrategy) {
        this.capacity = capacity;
        this.periodInNanos = unit.toNanos(period);
        this.refillStrategy = refillStrategy;
        this.waitingStrategy = waitingStrategy;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getPeriodInNanos() {
        return periodInNanos;
    }

    public RefillStrategy getRefillStrategy() {
        return refillStrategy;
    }

    public WaitingStrategy getWaitingStrategy() {
        return waitingStrategy;
    }

}