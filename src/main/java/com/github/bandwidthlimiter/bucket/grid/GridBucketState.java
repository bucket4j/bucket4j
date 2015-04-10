package com.github.bandwidthlimiter.bucket.grid;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;

import java.io.Serializable;

public class GridBucketState implements Serializable {

    private final BucketConfiguration bucketConfiguration;
    private final BucketState bucketState;

    public GridBucketState() {
        bucketConfiguration = null;
        bucketState = null;
    }

    public GridBucketState(BucketConfiguration bucketConfiguration, BucketState bucketState) {
        this.bucketConfiguration = bucketConfiguration;
        this.bucketState = bucketState;
    }

    public BucketConfiguration getBucketConfiguration() {
        return bucketConfiguration;
    }

    public BucketState getBucketState() {
        return bucketState;
    }

}
