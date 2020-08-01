package io.github.bucket4j.distributed;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketListener;

import java.time.Duration;

public interface BucketProxy extends Bucket {

    @Override
    BucketProxy toListenable(BucketListener listener);

    /**
     * TODO javadocs
     */
    default void syncImmediately() {
        syncByCondition(0L, Duration.ZERO);
    }

    /**
     *
     * @param unsynchronizedTokens
     * @param timeSinceLastSync
     */
    void syncByCondition(long unsynchronizedTokens, Duration timeSinceLastSync);

}
