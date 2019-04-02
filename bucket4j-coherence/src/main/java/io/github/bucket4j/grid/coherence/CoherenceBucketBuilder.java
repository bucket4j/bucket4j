/*
 *
 *   Copyright 2015-2019 Vladimir Bukhtoyarov
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

package io.github.bucket4j.grid.coherence;

import com.tangosol.net.NamedCache;
import io.github.bucket4j.AbstractBucketBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.RecoveryStrategy;

import java.io.Serializable;

/**
 * {@inheritDoc}
 *
 * This builder creates the buckets backed by <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 *
 * @see io.github.bucket4j.grid.jcache.JCacheBucketBuilder
 *
 */
public class CoherenceBucketBuilder extends AbstractBucketBuilder<CoherenceBucketBuilder> {

    /**
     * Creates the new instance of {@link CoherenceBucketBuilder}
     */
    public CoherenceBucketBuilder() {
        super();
    }

    /**
     * Constructs an instance of {@link GridBucket} which state actually stored inside in-memory data-grid,
     * semantic of this method is fully equals to {@link io.github.bucket4j.grid.jcache.JCacheBucketBuilder#build(javax.cache.Cache, Serializable, RecoveryStrategy)}
     *
     * @return new distributed bucket
     */
    public <K extends Serializable> Bucket build(NamedCache<K, GridBucketState> cache, K key, RecoveryStrategy recoveryStrategy) {
        BucketConfiguration configuration = buildConfiguration();
        CoherenceProxy<K> gridProxy = new CoherenceProxy<>(cache);
        return GridBucket.createInitializedBucket(key, configuration, gridProxy, recoveryStrategy);
    }

}
