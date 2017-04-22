
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

package io.github.bucket4j.local;

import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;

/**
 * This builder creates in-memory buckets ({@link LockFreeBucket}).
 */
public class LocalBucketBuilder extends ConfigurationBuilder<LocalBucketBuilder> {

    /**
     * Constructs the bucket using {@link SynchronizationStrategy#LOCK_FREE} synchronization strategy.
     *
     * @return the new bucket
     */
    public Bucket build() {
        return build(SynchronizationStrategy.LOCK_FREE);
    }

    /**
     * Constructs the new instance of local bucket which concrete type depends on synchronizationStrategy
     *
     * @param synchronizationStrategy the strategy of synchronization which need to be applied to prevent data-races in multithreading usage scenario.
     *
     * @return the new bucket
     */
    public Bucket build(SynchronizationStrategy synchronizationStrategy) {
        BucketConfiguration configuration = buildConfiguration();
        switch (synchronizationStrategy) {
            case LOCK_FREE: return new LockFreeBucket(configuration);
            case SYNCHRONIZED: return new SynchronizedBucket(configuration);
            case NONE: return new UnsafeBucket(configuration);
            default: throw new IllegalStateException();
        }
    }

}
