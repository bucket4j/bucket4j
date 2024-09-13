package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;

/**
 * Provider for {@link BucketListener}
 *
 * @param <K> type of key
 */
public interface BucketListenerProvider<K> {

    BucketListenerProvider<?> DEFAULT = bucketKey -> BucketListener.NOPE;

    /**
     * Provides listener for bucket.
     *
     * <p>
     * This method is called by {@link ProxyManager} each time when TODO.
     *
     * @param bucketKey the bucketKey
     *
     * @return listener for bucket with specified bucketKey
     */
    BucketListener get(K bucketKey);

}
