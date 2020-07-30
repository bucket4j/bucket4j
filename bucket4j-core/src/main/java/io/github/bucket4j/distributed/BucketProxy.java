package io.github.bucket4j.distributed;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketListener;

public interface BucketProxy extends Bucket {

    @Override
    BucketProxy toListenable(BucketListener listener);

    /**
     * TODO javadocs
     */
    void sync();

}
