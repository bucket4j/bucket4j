
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

package com.github.bucket4j.grid.jcache;

import com.github.bucket4j.AbstractBucketBuilder;
import com.github.bucket4j.Bucket;
import com.github.bucket4j.BucketConfiguration;
import com.github.bucket4j.grid.GridBucket;
import com.github.bucket4j.grid.GridBucketState;
import com.github.bucket4j.grid.RecoveryStrategy;

import javax.cache.Cache;
import java.io.Serializable;

/**
 *  {@inheritDoc}
 *
 * This builder creates the buckets backed by any <a href="https://www.jcp.org/en/jsr/detail?id=107">JCache API (JSR 107)</a> implementation.
 *
 */
public class JCacheBucketBuilder extends AbstractBucketBuilder<JCacheBucketBuilder> {

    private final RecoveryStrategy recoveryStrategy;

    /**
     * Creates the new instance of {@link JCacheBucketBuilder} with configured recovery strategy
     *
     * @param recoveryStrategy specifies the reaction which should be applied in case of previously saved state of bucket has been lost.
     */
    public JCacheBucketBuilder(RecoveryStrategy recoveryStrategy) {
        this.recoveryStrategy = recoveryStrategy;
    }

    /**
     * Constructs an instance of {@link com.github.bucket4j.grid.GridBucket} which responsible to limit rate inside Apache Ignite(GridGain) cluster.
     *
     * @param cache distributed cache which will hold bucket inside cluster.
     *             Feel free to store inside single {@code cache} as mush buckets as you need.
     * @param key  for storing bucket inside {@code cache}.
     *             If you plan to store multiple buckets inside single {@code cache}, then each bucket should has own unique {@code key}.
     *
     * @see JCacheProxy
     */
    public <K extends Serializable> Bucket build(Cache<K, GridBucketState> cache, K key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new JCacheProxy(cache, key), recoveryStrategy);
    }

}
