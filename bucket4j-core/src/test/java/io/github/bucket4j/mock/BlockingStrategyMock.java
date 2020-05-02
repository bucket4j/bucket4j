
package io.github.bucket4j.mock;

import io.github.bucket4j.BlockingStrategy;
import io.github.bucket4j.UninterruptibleBlockingStrategy;

public class BlockingStrategyMock implements BlockingStrategy, UninterruptibleBlockingStrategy {

    private final TimeMeterMock meterMock;
    private long parkedNanos = 0;
    private long atemptToParkNanos = 0;

    public BlockingStrategyMock(TimeMeterMock meterMock) {
        this.meterMock = meterMock;
    }

    public long getParkedNanos() {
        return parkedNanos;
    }

    public long getAtemptToParkNanos() {
        return atemptToParkNanos;
    }

    @Override
    public void park(long nanosToPark) throws InterruptedException {
        atemptToParkNanos += nanosToPark;
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        parkedNanos += nanosToPark;
        meterMock.addTime(nanosToPark);
    }

    @Override
    public void parkUninterruptibly(long nanosToPark) {
        atemptToParkNanos += nanosToPark;
        parkedNanos += nanosToPark;
        meterMock.addTime(nanosToPark);
    }

}