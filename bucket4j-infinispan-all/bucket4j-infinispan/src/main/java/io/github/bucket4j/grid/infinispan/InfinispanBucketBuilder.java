

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.AbstractBucketBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.RecoveryStrategy;
import org.infinispan.functional.FunctionalMap;

import javax.cache.Cache;
import java.io.Serializable;

/**
 * {@inheritDoc}
 *
 * This builder creates the buckets backed by <a href="http://infinispan.org/"></a> in-memory data store.
 *
 * @see io.github.bucket4j.grid.jcache.JCacheBucketBuilder
 *
 */
public class InfinispanBucketBuilder extends AbstractBucketBuilder<InfinispanBucketBuilder> {

    /**
     * Creates the new instance of {@link InfinispanBucketBuilder}
     */
    public InfinispanBucketBuilder() {
        super();
    }

    /**
     * Constructs an instance of {@link GridBucket} which state actually stored inside in-memory data-grid,
     * semantic of this method is fully equals to {@link io.github.bucket4j.grid.jcache.JCacheBucketBuilder#build(Cache, Serializable, RecoveryStrategy)}
     *
     * @return new distributed bucket
     */
    public <K extends Serializable> Bucket build(FunctionalMap.ReadWriteMap<K, GridBucketState> readWriteMap, K key, RecoveryStrategy recoveryStrategy) {
        BucketConfiguration configuration = buildConfiguration();
        InfinispanProxy<K> gridProxy = new InfinispanProxy<>(readWriteMap);
        return GridBucket.createInitializedBucket(key, configuration, gridProxy, recoveryStrategy);
    }

}
