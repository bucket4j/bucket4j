
package io.github.bucket4j.mock;

import io.github.bucket4j.*;
import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.RecoveryStrategy;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.local.SynchronizationStrategy;

import java.util.ArrayList;
import java.util.List;

import static io.github.bucket4j.grid.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;

public enum BucketType {

    LOCAL_LOCK_FREE {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder, TimeMeter timeMeter) {
            return ((LocalBucketBuilder) builder)
                    .withCustomTimePrecision(timeMeter)
                    .build();
        }
    },
    LOCAL_SYNCHRONIZED {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder, TimeMeter timeMeter) {
            return ((LocalBucketBuilder) builder)
                    .withCustomTimePrecision(timeMeter)
                    .withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED)
                    .build();
        }
    },
    LOCAL_UNSAFE {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder, TimeMeter timeMeter) {
            return ((LocalBucketBuilder) builder)
                    .withCustomTimePrecision(timeMeter)
                    .withSynchronizationStrategy(SynchronizationStrategy.NONE)
                    .build();
        }
    },
    GRID {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder, TimeMeter timeMeter) {
            BucketConfiguration configuration = PackageAcessor.buildConfiguration(builder);
            GridProxyMock gridProxy = new GridProxyMock(timeMeter);
            return GridBucket.createInitializedBucket(42, configuration, gridProxy, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        }
    };

    abstract public Bucket createBucket(AbstractBucketBuilder builder, TimeMeter timeMeter);

    public Bucket createBucket(AbstractBucketBuilder builder) {
        return createBucket(builder, TimeMeter.SYSTEM_MILLISECONDS);
    }

}
