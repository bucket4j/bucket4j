package com.github.bandwidthlimiter.leakybucket;

public interface LeakyBucketState {

    long getCurrentSize(Bandwidth bandwidth);

    void setCurrentSize(Bandwidth bandwidth, long size);

    long getRefillState(LeakyBucketConfiguration configuration, int offset);

    void setRefillState(LeakyBucketConfiguration configuration, int offset, long value);

}