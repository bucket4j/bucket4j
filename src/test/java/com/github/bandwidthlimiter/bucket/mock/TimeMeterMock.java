package com.github.bandwidthlimiter.bucket.mock;

import com.github.bandwidthlimiter.bucket.TimeMeter;

/**
 * Created by vladimir.bukhtoyarov on 09.04.2015.
 */
public class TimeMeterMock implements TimeMeter {

    private long currentTime;
    private long sleeped = 0;

    public TimeMeterMock() {
        currentTime = 0;
    }

    public TimeMeterMock(long currentTime) {
        this.currentTime = currentTime;
    }

    public void addTime(long duration) {
        currentTime += duration;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public long getSleeped() {
        return sleeped;
    }

    @Override
    public long currentTime() {
        return currentTime;
    }

    @Override
    public void sleep(long units) throws InterruptedException {
        sleeped += units;
    }

}
