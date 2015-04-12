package com.github.bandwidthlimiter.bucket;

public class AdjusterMock implements BandwidthAdjuster {

    private long capacity = 0;

    public AdjusterMock(long capacity) {
        this.capacity = capacity;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

}
