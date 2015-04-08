package com.github.bandwidthlimiter.leakybucket;

public interface LeakyBucketState {

    long getCurrentSize(int bandwidthIndex);

    void setCurrentSize(int bandwidthIndex, long size);

    long getRefillMarker(LeakyBucketConfiguration configuration, int idx);

    void setRefillMarker(LeakyBucketConfiguration configuration, int idx, long refillMarker);

}