package com.github.bucket4j;

import com.github.bucket4j.grid.jcache.JCacheBucketBuilder;
import com.github.bucket4j.grid.RecoveryStrategy;
import com.github.bucket4j.local.LocalBucketBuilder;

/**
 * Entry point of bucket4j library
 */
public class Bucket4j {

    public static LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }

    public static JCacheBucketBuilder jCacheBuilder(RecoveryStrategy recoveryStrategy) {
        return new JCacheBucketBuilder(recoveryStrategy);
    }

}
