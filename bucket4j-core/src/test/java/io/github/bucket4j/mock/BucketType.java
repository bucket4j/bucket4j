
package io.github.bucket4j.mock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManagerConfig;
import io.github.bucket4j.distributed.proxy.synchronization.batch.BatchingSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronizations;
import io.github.bucket4j.local.ConcurrencyStrategy;
import io.github.bucket4j.local.LocalBucketBuilder;

public enum BucketType {

    LOCAL_LOCK_FREE {
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

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAsyncModeSupported() {
            return false;
        }

    },
    LOCAL_SYNCHRONIZED {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder
                .withCustomTimePrecision(timeMeter)
                .withSynchronizationStrategy(ConcurrencyStrategy.SYNCHRONIZED)
                .withListener(listener)
                .build();
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAsyncModeSupported() {
            return false;
        }

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            throw new UnsupportedOperationException();
        }

    },
    LOCAL_REENTRANT_LOCK_PROTECTED {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder
                .withCustomTimePrecision(timeMeter)
                .withSynchronizationStrategy(ConcurrencyStrategy.REENTRANT_LOCK_PROTECTED)
                .withListener(listener)
                .build();
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAsyncModeSupported() {
            return false;
        }

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            throw new UnsupportedOperationException();
        }

    },
    LOCAL_UNSAFE {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            LocalBucketBuilder builder = Bucket.builder();
            for (Bandwidth bandwidth : configuration.getBandwidths()) {
                builder.addLimit(bandwidth);
            }
            return builder
                .withCustomTimePrecision(timeMeter)
                .withSynchronizationStrategy(ConcurrencyStrategy.UNSAFE)
                .withListener(listener)
                .build();
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAsyncModeSupported() {
            return false;
        }

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            throw new UnsupportedOperationException();
        }
    },
    GRID {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            return createProxyManager(timeMeter).builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new ProxyManagerMock<>(timeMeter);
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            return createAsyncProxyManager(timeMeter).builder()
                .withListener(listener)
                .build(42, () -> CompletableFuture.completedFuture(configuration));
        }

        @Override
        public boolean isAsyncModeSupported() {
            return true;
        }

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            return new AsyncProxyManagerMock<>(timeMeter);
        }
    },
    GRID_WITH_PER_BUCKET_BATCHING_SYNCHRONIZATION {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            return createProxyManager(timeMeter).builder()
                .withSynchronization(BucketSynchronizations.batching())
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new ProxyManagerMock<>(timeMeter);
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            return createAsyncProxyManager(timeMeter).builder()
                    .withSynchronization(BucketSynchronizations.batching())
                    .withListener(listener)
                    .build(42, () -> CompletableFuture.completedFuture(configuration));
        }

        @Override
        public boolean isAsyncModeSupported() {
            return true;
        }

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            return new AsyncProxyManagerMock<>(timeMeter);
        }
    },
    GRID_WITH_PER_MANAGER_BATCHING_SYNCHRONIZATION {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            return createProxyManager(timeMeter).builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new ProxyManagerMock<>(ProxyManagerConfig.getDefault()
                .withClientClock(timeMeter)
                .withSynchronization(new BatchingSynchronization())
            );
        }

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            return new AsyncProxyManagerMock<>(ProxyManagerConfig.getDefault()
                .withClientClock(timeMeter)
                .withSynchronization(new BatchingSynchronization())
            );
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            return createAsyncProxyManager(timeMeter).builder()
                .withListener(listener)
                .build(42, () -> CompletableFuture.completedFuture(configuration));
        }

        @Override
        public boolean isAsyncModeSupported() {
            return true;
        }
    },

    COMPARE_AND_SWAP {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            CompareAndSwapBasedProxyManagerMock<Integer> proxyManager = new CompareAndSwapBasedProxyManagerMock<>(ProxyManagerConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new CompareAndSwapBasedProxyManagerMock<>(ProxyManagerConfig.getDefault().withClientClock(timeMeter));
        }

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            return new AsyncCompareAndSwapBasedProxyManagerMock<>(ProxyManagerConfig.getDefault().withClientClock(timeMeter));
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            AsyncCompareAndSwapBasedProxyManagerMock<Integer> proxyManager = new AsyncCompareAndSwapBasedProxyManagerMock<>(ProxyManagerConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                .withListener(listener)
                .build(42, () -> CompletableFuture.completedFuture(configuration));
        }

        @Override
        public boolean isAsyncModeSupported() {
            return true;
        }
    },

    LOCK_BASED {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            LockBasedProxyManagerMock<Integer> proxyManager = new LockBasedProxyManagerMock<>(ProxyManagerConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new LockBasedProxyManagerMock<>(ProxyManagerConfig.getDefault().withClientClock(timeMeter));
        }

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAsyncModeSupported() {
            return false;
        }
    },

    SELECT_FOR_UPDATE {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            SelectForUpdateBasedProxyManagerMock<Integer> proxyManager = new SelectForUpdateBasedProxyManagerMock<>(ProxyManagerConfig.getDefault().withClientClock(timeMeter));
            return proxyManager.builder()
                .withListener(listener)
                .build(42, () -> configuration);
        }

        @Override
        public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter) {
            return new LockBasedProxyManagerMock<>(ProxyManagerConfig.getDefault().withClientClock(timeMeter));
        }

        @Override
        public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAsyncModeSupported() {
            return false;
        }
    };

    public static final List<BucketType> ASYNC_TYPES = Stream.of(values()).filter(BucketType::isAsyncModeSupported).toList();

    public final Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
        return createBucket(configuration, timeMeter, BucketListener.NOPE);
    }

    abstract public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener);

    abstract public ProxyManager<Integer> createProxyManager(TimeMeter timeMeter);

    abstract public AsyncProxyManager<Integer> createAsyncProxyManager(TimeMeter timeMeter);

    abstract public AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter, BucketListener listener);

    public Bucket createBucket(BucketConfiguration configuration) {
        return createBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public final AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
        return createAsyncBucket(configuration, timeMeter, BucketListener.NOPE);
    }

    public final AsyncBucketProxy createAsyncBucket(BucketConfiguration configuration) {
        return createAsyncBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public boolean isLocal() {
        return this == LOCAL_LOCK_FREE || this == LOCAL_SYNCHRONIZED || this == LOCAL_UNSAFE || this == LOCAL_REENTRANT_LOCK_PROTECTED;
    }

    public abstract boolean isAsyncModeSupported();
}
