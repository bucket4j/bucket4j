
package io.github.bucket4j;

import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.local.LockFreeBucket;
import io.github.bucket4j.local.SynchronizedBucket;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.GridProxyMock;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class PackageAcessor {

    public static BucketConfiguration buildConfiguration(AbstractBucketBuilder builder) {
        return builder.buildConfiguration();
    }

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
