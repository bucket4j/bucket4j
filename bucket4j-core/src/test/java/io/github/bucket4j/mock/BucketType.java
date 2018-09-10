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
import io.github.bucket4j.local.InMemory;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.local.SynchronizationStrategy;
import io.github.bucket4j.remote.BucketProxy;

import static io.github.bucket4j.remote.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION;

public enum BucketType {

    LOCAL_LOCK_FREE {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder, TimeMeter timeMeter) {
            return ((LocalBucketBuilder) builder)
                    .withCustomTimePrecision(timeMeter)
                    .build();
        }

        @Override
        public Extension getExtension() {
            return InMemory.INSTANCE;
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

        @Override
        public Extension getExtension() {
            return InMemory.INSTANCE;
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

        @Override
        public Extension getExtension() {
            return InMemory.INSTANCE;
        }
    },
    GRID {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder, TimeMeter timeMeter) {
            BucketConfiguration configuration = PackageAcessor.buildConfiguration(builder);
            BackendMock gridProxy = new BackendMock(timeMeter);
            return BucketProxy.createInitializedBucket(42, configuration, gridProxy, THROW_BUCKET_NOT_FOUND_EXCEPTION);
        }

        @Override
        public Extension getExtension() {
            return new RemoteExtensionMock();
        }
    };

    abstract public Bucket createBucket(AbstractBucketBuilder builder, TimeMeter timeMeter);

    abstract public Extension getExtension();

    public Bucket createBucket(AbstractBucketBuilder builder) {
        return createBucket(builder, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public static class RemoteExtensionMock implements Extension {
        @Override
        public AbstractBucketBuilder builder() {
            return null;
        }
    }

}
