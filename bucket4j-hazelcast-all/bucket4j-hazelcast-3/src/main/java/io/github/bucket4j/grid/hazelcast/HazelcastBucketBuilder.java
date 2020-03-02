package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.core.IMap;
import io.github.bucket4j.AbstractBucketBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.RecoveryStrategy;

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
public class HazelcastBucketBuilder extends AbstractBucketBuilder<HazelcastBucketBuilder> {

    /**
     * Creates the new instance of {@link HazelcastBucketBuilder}
     */
    public HazelcastBucketBuilder() {
        super();
    }

    /**
     * Constructs an instance of {@link GridBucket} which state actually stored inside in-memory data-grid,
     * semantic of this method is fully equals to {@link io.github.bucket4j.grid.jcache.JCacheBucketBuilder#build(Cache, Serializable, RecoveryStrategy)}
     *
     * @return new distributed bucket
     */
    public <K extends Serializable> Bucket build(IMap<K, GridBucketState> map, K key, RecoveryStrategy recoveryStrategy) {
        BucketConfiguration configuration = buildConfiguration();
        HazelcastProxy<K> gridProxy = new HazelcastProxy<>(map);
        return GridBucket.createInitializedBucket(key, configuration, gridProxy, recoveryStrategy);
    }

}
