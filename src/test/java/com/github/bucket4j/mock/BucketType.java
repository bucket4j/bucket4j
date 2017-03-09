/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j.mock;

import com.github.bucket4j.AbstractBucketBuilder;
import com.github.bucket4j.Bucket;
import com.github.bucket4j.BucketConfiguration;
import com.github.bucket4j.grid.GridBucket;
import com.github.bucket4j.grid.RecoveryStrategy;
import com.github.bucket4j.local.LocalBucketBuilder;
import com.github.bucket4j.local.SynchronizationStrategy;

import java.util.ArrayList;
import java.util.List;

public enum BucketType {

    LOCAL_LOCK_FREE {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder) {
            return ((LocalBucketBuilder) builder).build();
        }
    },
    LOCAL_SYNCHRONIZED {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder) {
            return ((LocalBucketBuilder) builder).build(SynchronizationStrategy.SYNCHRONIZED);
        }
    },
    LOCAL_UNSAFE {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder) {
            return ((LocalBucketBuilder) builder).build(SynchronizationStrategy.NONE);
        }
    },
    GRID {
        @Override
        public Bucket createBucket(AbstractBucketBuilder builder) {
            BucketConfiguration configuration = builder.createConfiguration();
            return new GridBucket(configuration, new GridProxyMock(), RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION);
        }
    };

    abstract public Bucket createBucket(AbstractBucketBuilder builder);

    public static List<Bucket> createBuckets(AbstractBucketBuilder builder) {
        List<Bucket> buckets = new ArrayList<>();
        for (BucketType type : values()) {
            Bucket bucket = type.createBucket(builder);
            buckets.add(bucket);
        }
        return buckets;
    }

}
