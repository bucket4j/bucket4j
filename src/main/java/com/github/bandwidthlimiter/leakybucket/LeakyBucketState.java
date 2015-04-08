package com.github.bandwidthlimiter.leakybucket;

public interface LeakyBucketState {

    long getCurrentSize(int bandwidthIndex);

    void setCurrentSize(int bandwidthIndex, long size);

    long getRefillState(LeakyBucketConfiguration configuration, int idx);

    void setRefillState(LeakyBucketConfiguration configuration, int idx, long refillMarker);

}