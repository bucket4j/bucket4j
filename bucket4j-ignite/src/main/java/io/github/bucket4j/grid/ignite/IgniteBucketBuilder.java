package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.AbstractBucketBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.RecoveryStrategy;
import org.apache.ignite.IgniteCache;

import javax.cache.Cache;
import java.io.Serializable;

/**
 * {@inheritDoc}
 *
 * This builder creates the buckets backed by <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 *
 * @see io.github.bucket4j.grid.jcache.JCacheBucketBuilder
 *
 */
public class IgniteBucketBuilder extends AbstractBucketBuilder<IgniteBucketBuilder> {

    /**
     * Creates the new instance of {@link IgniteBucketBuilder}
     */
    public IgniteBucketBuilder() {
        super();
    }

    /**
     * Constructs an instance of {@link GridBucket} which state actually stored inside in-memory data-grid,
     * semantic of this method is fully equals to {@link io.github.bucket4j.grid.jcache.JCacheBucketBuilder#build(Cache, Serializable, RecoveryStrategy)}
     *
     * @return new distributed bucket
     */
    public <K extends Serializable> Bucket build(IgniteCache<K, GridBucketState> cache, K key, RecoveryStrategy recoveryStrategy) {
        BucketConfiguration configuration = buildConfiguration();
        IgniteProxy<K> gridProxy = new IgniteProxy<>(cache);
        return GridBucket.createInitializedBucket(key, configuration, gridProxy, recoveryStrategy);
    }

}
