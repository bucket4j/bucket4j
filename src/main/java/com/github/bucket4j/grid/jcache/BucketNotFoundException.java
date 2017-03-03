package com.github.bucket4j.grid.jcache;

/**
 * Created by vladimir.bukhtoyarov on 03.03.2017.
 */
public class BucketNotFoundException extends IllegalStateException {

    private final String cacheName;
    private final Object bucketId;

    public BucketNotFoundException(String cacheName, Object bucketId) {
        super(createErrorMessage(cacheName, bucketId));
        this.cacheName = cacheName;
        this.bucketId = bucketId;
    }

    private static String createErrorMessage(String cacheName, Object bucketId) {
        return "Cache [" + cacheName + "]" + " does not contain bucket with key [" + bucketId + "]";
    }

    public Object getBucketId() {
        return bucketId;
    }

    public String getCacheName() {
        return cacheName;
    }

}
