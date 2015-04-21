package com.github.bandwidthlimiter.state;

import org.openjdk.jmh.annotations.*;

@State(Scope.Thread)
public class ThreadDistributionState {

    long threadId = Thread.currentThread().getId();

    public long invocationCount;

    @Setup
    public void setUp() {
        threadId = Thread.currentThread().getId();
    }

    @TearDown(Level.Iteration)
    public void printDistribution() {
        System.out.print(this);
        invocationCount = 0;
    }

    @Override
    public String toString() {
        return "\nThreadDistributionState{" +
                "threadId=" + threadId +
                ", invocationCount=" + invocationCount +
                '}';
    }

}
