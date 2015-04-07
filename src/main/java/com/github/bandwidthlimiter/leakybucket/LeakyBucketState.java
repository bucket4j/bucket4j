package com.github.bandwidthlimiter.leakybucket;

public interface LeakyBucketState {

    long getRefillMarker(int bandwidthIndex);

    void setRefillMarker(int bandwidthIndex, long refillMarker);

    long getCurrentSize(int bandwidthIndex);

    void setCurrentSize(int bandwidthIndex, long size);

}