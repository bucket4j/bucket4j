package com.github.bandwidthlimiter.bucket;

/**
 * Created by vladimir.bukhtoyarov on 09.04.2015.
 */
public class TimeMeterMock implements TimeMeter {

    private long currentTime;

    public TimeMeterMock() {
        currentTime = 0;
    }

    public TimeMeterMock(long currentTime) {
        this.currentTime = currentTime;
    }

    public void addTime(long duration) {
        currentTime += duration;
    }

    @Override
    public long currentTime() {
        return currentTime;
    }

    @Override
    public void sleep(long units) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

}
