
package io.github.bucket4j.mock;

import java.util.concurrent.CompletableFuture;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.AsyncBucketProxyAdapter;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.optimization.Optimizations;
import io.github.bucket4j.distributed.proxy.synchronization.batch.BatchingSynchronization;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.local.SynchronizationStrategy;

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
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder
                .withCustomTimePrecision(timeMeter)
                .withListener(listener)
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
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder
                .withCustomTimePrecision(timeMeter)
                .withSynchronizationStrategy(SynchronizationStrategy.SYNCHRONIZED)
                .withListener(listener)
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
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder
                .withCustomTimePrecision(timeMeter)
                .withSynchronizationStrategy(SynchronizationStrategy.NONE)
                .withListener(listener)
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
            return proxyManager.builder().build(42, () -> configuration);
        }

        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new ProxyManagerMock<>(timeMeter);
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.asAsync().builder()
                    .build(42, () -> CompletableFuture.completedFuture(configuration));
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.asAsync().builder()
                .withListener(listener)
                .build(42, () -> CompletableFuture.completedFuture(configuration));
        }
    },
    GRID_WITH_BATCHING_OPTIMIZATION {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.builder()
                .withOptimization(Optimizations.batching())
                .build(42, () -> configuration);
        }

        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.builder()
                .withOptimization(Optimizations.batching())
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(timeMeter);
            return proxyManager.asAsync().builder()
                    .withOptimization(Optimizations.batching())
                    .build(42, () -> CompletableFuture.completedFuture(configuration));
        }
    },
    GRID_WITH_PER_MANAGER_BATCHING_OPTIMIZATION {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(ClientSideConfig.getDefault()
                .withClientClock(timeMeter)
                .withSynchronization(new BatchingSynchronization())
            );
            return proxyManager.builder()
                .withOptimization(Optimizations.batching())
                .build(42, () -> configuration);
        }

        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(ClientSideConfig.getDefault()
                .withClientClock(timeMeter)
                .withSynchronization(new BatchingSynchronization())
            );
            return proxyManager.builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            ProxyManagerMock<Integer> proxyManager = new ProxyManagerMock<>(ClientSideConfig.getDefault()
                .withClientClock(timeMeter)
                .withSynchronization(new BatchingSynchronization())
            );
            return proxyManager.asAsync().builder()
                .withOptimization(Optimizations.batching())
                .build(42, () -> CompletableFuture.completedFuture(configuration));
        }
    },

    COMPARE_AND_SWAP {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            CompareAndSwapBasedProxyManagerMock<Integer> proxyManager = new CompareAndSwapBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                    .build(42, () -> configuration);
        }

        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            CompareAndSwapBasedProxyManagerMock<Integer> proxyManager = new CompareAndSwapBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new CompareAndSwapBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            CompareAndSwapBasedProxyManagerMock<Integer> proxyManager = new CompareAndSwapBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.asAsync().builder()
                .build(42, () -> CompletableFuture.completedFuture(configuration));
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            CompareAndSwapBasedProxyManagerMock<Integer> proxyManager = new CompareAndSwapBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.asAsync().builder()
                .withListener(listener)
                .build(42, () -> CompletableFuture.completedFuture(configuration));
        }
    },

    LOCK_BASED {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            LockBasedProxyManagerMock<Integer> proxyManager = new LockBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                    .build(42, () -> configuration);
        }

        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            LockBasedProxyManagerMock<Integer> proxyManager = new LockBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new LockBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
        }
    },

    SELECT_FOR_UPDATE {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            SelectForUpdateBasedProxyManagerMock<Integer> proxyManager = new SelectForUpdateBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                .build(42, () -> configuration);
        }

        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            SelectForUpdateBasedProxyManagerMock<Integer> proxyManager = new SelectForUpdateBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new LockBasedProxyManagerMock<>(ClientSideConfig.getDefault().withClientClock(timeMeter));
        }
    };

    abstract public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter);

    abstract public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener);

    abstract public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter);

    public Bucket createBucket(BucketConfiguration configuration) {
        return createBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
        Bucket bucket = createBucket(configuration, timeMeter);
        return AsyncBucketProxyAdapter.fromSync(bucket);
    }

    public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
        Bucket bucket = createBucket(configuration, timeMeter, listener);
        return AsyncBucketProxyAdapter.fromSync(bucket);
    }

    public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration) {
        return createAsyncBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public boolean isLocal() {
        return this == LOCAL_LOCK_FREE || this == LOCAL_SYNCHRONIZED || this == LOCAL_UNSAFE;
    }

}
