package com.github.bandwidthlimiter.bucket.mock;

import com.github.bandwidthlimiter.bucket.Bucket;
import com.github.bandwidthlimiter.bucket.BucketBuilder;

import java.util.ArrayList;
import java.util.List;

public enum BucketType {

    LOCAL_UNSAFE {
        @Override
        public Bucket createBucket(BucketBuilder builder) {
            return builder.buildLocalNonSynchronized();
        }
    },
    LOCAL_THREADSAFE {
        @Override
        public Bucket createBucket(BucketBuilder builder) {
            return builder.build();
        }
    },
    GRID {
        @Override
        public Bucket createBucket(BucketBuilder builder) {
            return builder.buildCustomGrid(new GridProxyMock());
        }
    };

    abstract public Bucket createBucket(BucketBuilder builder);

    public static List<Bucket> createBuckets(BucketBuilder builder) {
        List<Bucket> buckets = new ArrayList<>();
        for (BucketType type : values()) {
            Bucket bucket = type.createBucket(builder);
            buckets.add(bucket);
        }
        return buckets;
    }

}
