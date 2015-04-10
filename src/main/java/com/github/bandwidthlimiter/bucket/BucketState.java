package com.github.bandwidthlimiter.bucket;

public interface BucketState {

    long getCurrentSize(Bandwidth bandwidth);

    void setCurrentSize(Bandwidth bandwidth, long size);

    long getRefillState(BucketConfiguration configuration, int offset);

    void setRefillState(BucketConfiguration configuration, int offset, long value);

}