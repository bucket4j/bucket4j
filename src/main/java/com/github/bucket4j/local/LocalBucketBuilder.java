
/*
 *  Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.github.bucket4j.local;

import com.github.bucket4j.AbstractBucketBuilder;
import com.github.bucket4j.Bucket;
import com.github.bucket4j.BucketConfiguration;

/**
 * This builder creates in-memory buckets ({@link LockFreeBucket}).
 */
public class LocalBucketBuilder extends AbstractBucketBuilder<LocalBucketBuilder> {

    /**
     * Constructs an instance of {@link com.github.bucket4j.local.LockFreeBucket}
     *
     * @return an instance of {@link com.github.bucket4j.local.LockFreeBucket}
     */
    public Bucket build() {
        return build(SynchronizationStrategy.LOCK_FREE);
    }

    public Bucket build(SynchronizationStrategy strategy) {
        BucketConfiguration configuration = createConfiguration();
        switch (strategy) {
            case LOCK_FREE: return new LockFreeBucket(configuration);
            case SYNCHRONIZED: return new SynchronizedBucket(configuration);
            case NONE: return new UnsafeBucket(configuration);
            default: throw new IllegalStateException();
        }
    }

}
