package io.github.bucket4j.local;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;

/**
 * Represents the bucket inside current JVM.
 */
public interface LocalBucket extends Bucket {

    /**
     * Returns configuration of this bucket.
     *
     * @return configuration
     */
    BucketConfiguration getConfiguration();

}
