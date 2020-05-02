
package io.github.bucket4j.distributed.remote;

/**
 * TODO javadocs
 */
public interface MutableBucketEntry {

    boolean exists();

    void set(RemoteBucketState state);

    RemoteBucketState get();

    public static BucketState getState(Bucket bucket) {
        if (bucket instanceof LockFreeBucket) {
            AtomicReference stateRef = getFieldValue(bucket, "stateRef");
            return getFieldValue(stateRef.get(), "state");
        } else if (bucket instanceof SynchronizedBucket) {
            return getFieldValue(bucket, "state");
        } else if (bucket instanceof GridBucket) {
            GridProxyMock proxy = getFieldValue(bucket, "gridProxy");
            GridBucketState gridState = getFieldValue(proxy, "state");
            return gridState.getState();
        } else {
            throw new IllegalStateException("Unknown bucket type " + bucket.getClass());
        }
    }

    private static <T> T getFieldValue(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
