package com.github.bucket4j.grid.jcache;

import com.github.bucket4j.AbstractBucketBuilder;
import com.github.bucket4j.Bucket;
import com.github.bucket4j.BucketConfiguration;
import com.github.bucket4j.grid.GridBucket;
import com.github.bucket4j.grid.GridBucketState;

import javax.cache.Cache;

/**
 * Created by vladimir.bukhtoyarov on 03.03.2017.
 */
public class JCacheBucketBuilder extends AbstractBucketBuilder<JCacheBucketBuilder> {

    private final RecoveryStrategy recoveryStrategy;

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
    public <K> Bucket build(Cache<K, GridBucketState> cache, K key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new JCacheProxy(cache, key));
    }

}
