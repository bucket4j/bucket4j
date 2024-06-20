package io.github.bucket4j.util;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.distributed.proxy.DefaultBucketProxy;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.local.LockFreeBucket;
import io.github.bucket4j.local.ReentrantLockProtectedBucket;
import io.github.bucket4j.local.SynchronizedBucket;
import io.github.bucket4j.local.ThreadUnsafeBucket;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class PackageAccessor {

    public static BucketState getState(Bucket bucket) {
        if (bucket instanceof LockFreeBucket) {
            AtomicReference<BucketState> stateRef = getFieldValue(bucket, "stateRef");
            return stateRef.get();
        } else if (bucket instanceof ReentrantLockProtectedBucket || bucket instanceof ThreadUnsafeBucket || bucket instanceof SynchronizedBucket) {
            return getFieldValue(bucket, "state");
        } else if (bucket instanceof DefaultBucketProxy) {
            DefaultBucketProxy proxy = getFieldValue(bucket, "gridProxy");
            RemoteBucketState gridState = getFieldValue(proxy, "state");
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
