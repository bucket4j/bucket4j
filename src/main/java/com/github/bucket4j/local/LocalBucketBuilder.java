package com.github.bucket4j.local;

import com.github.bucket4j.AbstractBucketBuilder;
import com.github.bucket4j.Bucket;
import com.github.bucket4j.BucketConfiguration;

/**
 * Created by vladimir.bukhtoyarov on 03.03.2017.
 */
public class LocalBucketBuilder extends AbstractBucketBuilder<LocalBucketBuilder> {

    /**
     * Constructs an instance of {@link com.github.bucket4j.local.LockFreeBucket}
     *
     * @return an instance of {@link com.github.bucket4j.local.LockFreeBucket}
     */
    public Bucket build() {
        BucketConfiguration configuration = createConfiguration();
        return new LockFreeBucket(configuration);
    }

}
