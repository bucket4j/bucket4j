
package io.github.bucket4j.mock;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.AsyncBucketProxyAdapter;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.optimization.Optimizations;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.local.SynchronizationStrategy;

import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;

public enum BucketType {

    LOCAL_LOCK_FREE {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder
                    .withCustomTimePrecision(timeMeter)
                    .build();
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

    },
    LOCAL_SYNCHRONIZED {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder
                    .withCustomTimePrecision(timeMeter)
                    .withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED)
                    .build();
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

    },
    LOCAL_UNSAFE {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder
                    .withCustomTimePrecision(timeMeter)
                    .withSynchronizationStrategy(SynchronizationStrategy.NONE)
                    .build();
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }
    },
    GRID {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .build(42, configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new ProxyManagerMock<>(timeMeter);
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.asAsync().builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .build(42, configuration);
        }
    },
    GRID_WITH_BATCHING_OPTIMIZATION {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .withOptimization(Optimizations.batching())
                    .build(42, configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.asAsync().builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .withOptimization(Optimizations.batching())
                    .build(42, configuration);
        }
    },
    COMPARE_AND_SWAP {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            CompareAndSwapBasedProxyManagerMock<Integer> proxyManager = new CompareAndSwapBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .build(42, configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new CompareAndSwapBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            CompareAndSwapBasedProxyManagerMock<Integer> proxyManager = new CompareAndSwapBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.asAsync().builder()
                .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                .build(42, configuration);
        }
    },

    LOCK_BASED {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            LockBasedProxyManagerMock<Integer> proxyManager = new LockBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .build(42, configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new LockBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
        }
    },

    SELECT_FOR_UPDATE {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            LockBasedProxyManagerMock<Integer> proxyManager = new LockBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .build(42, configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new LockBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
        }
    };

    abstract public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter);

    abstract public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter);

    public Bucket createBucket(BucketConfiguration configuration) {
        return createBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
        Bucket bucket = createBucket(configuration, timeMeter);
        return AsyncBucketProxyAdapter.fromSync(bucket);
    }

    public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration) {
        return createAsyncBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public boolean isLocal() {
        return this == LOCAL_LOCK_FREE || this == LOCAL_SYNCHRONIZED || this == LOCAL_UNSAFE;
    }

}
