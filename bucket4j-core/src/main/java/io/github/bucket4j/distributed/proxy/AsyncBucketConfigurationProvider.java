package io.github.bucket4j.distributed.proxy;

import java.util.concurrent.CompletableFuture;

import io.github.bucket4j.BucketConfiguration;

/**
 * Provider for {@link BucketConfiguration}
 *
 * @param <K> type of key
 */
public interface AsyncBucketConfigurationProvider<K> {

    AsyncBucketConfigurationProvider<?> DEFAULT = bucketKey -> {
        String msg = ""; // TODO
        throw new UnsupportedOperationException(msg);
    };

    /**
     * Provides configuration for bucket.
     *
     * <p>
     * This method is called by {@link ProxyManager} when it detects that bucket has not been persisted in remote storage.
     *
     * @param bucketKey the bucketKey
     *
     * @return configuration for bucket with specified bucketKey
     */
    CompletableFuture<BucketConfiguration> get(K bucketKey);

}