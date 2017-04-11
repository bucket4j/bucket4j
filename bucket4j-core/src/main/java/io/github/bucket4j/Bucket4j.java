
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

package io.github.bucket4j;

import io.github.bucket4j.grid.RecoveryStrategy;
import io.github.bucket4j.grid.jcache.JCacheBucketBuilder;
import io.github.bucket4j.local.LocalBucketBuilder;

/**
 * This is entry point for functionality provided bucket4j library.
 *
 * It is always better to initialize the buckets through this class.
 */
public class Bucket4j {

    /**
     * Creates the new builder of in-memory buckets.
     *
     * @return new instance of {@link LocalBucketBuilder}
     */
    public static LocalBucketBuilder builder() {
        return new LocalBucketBuilder();
    }

    /**
     * Creates the new builder for buckets backed by any <a href="https://www.jcp.org/en/jsr/detail?id=107">JCache API (JSR 107)</a> implementation.
     *
     * @param recoveryStrategy specifies the reaction which should be applied in case of previously saved state of bucket has been lost.
     *
     * @return new instance of {@link JCacheBucketBuilder}
     */
    public static JCacheBucketBuilder jCacheBuilder(RecoveryStrategy recoveryStrategy) {
        return new JCacheBucketBuilder(recoveryStrategy);
    }

}
