package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.BucketConfiguration;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface AsyncBackend<K> {

    RemoteAsyncBucketBuilder<K> builder();

    /**
     * TODO
     *
     * Locates configuration of bucket which actually stored outside current JVM.
     *
     * @param key the unique identifier used to point to the bucket in external storage.
     *
     * @return Optional surround the configuration or empty optional if bucket with specified key are not stored.
     */
    CompletableFuture<Optional<BucketConfiguration>> getProxyConfiguration(K key);

}
