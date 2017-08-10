/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.mock;

import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.RecoveryStrategy;
import io.github.bucket4j.local.LocalBucketBuilder;
import io.github.bucket4j.local.SynchronizationStrategy;

import java.util.ArrayList;
import java.util.List;

public enum BucketType {

    LOCAL_LOCK_FREE {
        @Override
        public Bucket createBucket(ConfigurationBuilder builder) {
            return ((LocalBucketBuilder) builder).build();
        }
    },
    LOCAL_SYNCHRONIZED {
        @Override
        public Bucket createBucket(ConfigurationBuilder builder) {
            return ((LocalBucketBuilder) builder).build(SynchronizationStrategy.SYNCHRONIZED);
        }
    },
    LOCAL_UNSAFE {
        @Override
        public Bucket createBucket(ConfigurationBuilder builder) {
            return ((LocalBucketBuilder) builder).build(SynchronizationStrategy.NONE);
        }
    },
    GRID {
        @Override
        public Bucket createBucket(ConfigurationBuilder builder) {
            BucketConfiguration configuration = builder.buildConfiguration();
            return GridBucket.createInitializedBucket(42, configuration, new GridProxyMock(), RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION);
        }
    };

    abstract public Bucket createBucket(ConfigurationBuilder builder, TimeMeter timeMeter);

    public static List<Bucket> createBuckets(ConfigurationBuilder builder) {
        List<Bucket> buckets = new ArrayList<>();
        for (BucketType type : values()) {
            Bucket bucket = type.createBucket(builder);
            buckets.add(bucket);
        }
        return buckets;
    }

}
