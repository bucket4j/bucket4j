/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.mock;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.AsyncBucket;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.local.SynchronizationStrategy;

import java.util.Arrays;
import java.util.List;

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
    },
    GRID {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            GridBackendMock<Integer> backend = new GridBackendMock<>(timeMeter);
            return backend.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildProxy(42, configuration);
        }

        @Override
        public AsyncBucket createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            GridBackendMock<Integer> backend = new GridBackendMock<>(timeMeter);
            return backend.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildAsyncProxy(42, configuration);
        }
    },
    COMPARE_AND_SWAP {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            CompareAndSwapBasedBackendMock<Integer> backend = new CompareAndSwapBasedBackendMock<>(timeMeter);
            return backend.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildProxy(42, configuration);
        }
    },
    SELECT_FOR_UPDATE {
        @Override
        public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
            LockBasedBackendMock<Integer> backend = new LockBasedBackendMock<>(timeMeter);
            return backend.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildProxy(42, configuration);
        }
    };

    public static List<BucketType> withAsyncSupport() {
        return Arrays.asList(GRID);
    }

    abstract public Bucket createBucket(BucketConfiguration configuration, TimeMeter timeMeter);

    public Bucket createBucket(BucketConfiguration configuration) {
        return createBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public AsyncBucket createAsyncBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
        throw new UnsupportedOperationException();
    }

    public AsyncBucket createAsyncBucket(BucketConfiguration configuration) {
        return createAsyncBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public boolean isAsyncModeSupported() {
        return BucketType.withAsyncSupport().contains(this);
    }

}
