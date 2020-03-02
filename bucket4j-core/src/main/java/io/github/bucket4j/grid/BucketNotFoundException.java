
package io.github.bucket4j.grid;

import java.io.Serializable;

/**
 * Exception which thrown each time when {@link GridBucket} found that bucket state has been lost,
 * and {@link GridBucket} is unable to repair bucket state or recovery strategy is {@link RecoveryStrategy#THROW_BUCKET_NOT_FOUND_EXCEPTION}.
 */
public class BucketNotFoundException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final Serializable bucketId;

    public BucketNotFoundException(Serializable bucketId) {
        super(createErrorMessage(bucketId));
        this.bucketId = bucketId;
    }

    private static String createErrorMessage(Serializable bucketId) {
        return "Bucket with key [" + bucketId + "] does not exist";
    }

    public Object getBucketId() {
        return bucketId;
    }

}
