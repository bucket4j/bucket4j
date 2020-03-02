

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridProxy;
import io.github.bucket4j.grid.ProxyManager;
import org.infinispan.functional.FunctionalMap;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Infinispan specific implementation of {@link ProxyManager}
 *
 * @param <K> type of key for buckets
 */
public class InfinispanProxyManager<K extends Serializable> implements ProxyManager<K> {

    private final GridProxy<K> gridProxy;

    InfinispanProxyManager(FunctionalMap.ReadWriteMap<K, GridBucketState> readWriteMap) {
        if (readWriteMap == null) {
            throw new IllegalArgumentException("map must not be null");
        }
        this.gridProxy = new InfinispanProxy<>(readWriteMap);
    }

    @Override
    public Bucket getProxy(K key, Supplier<BucketConfiguration> supplier) {
        return GridBucket.createLazyBucket(key, supplier, gridProxy);
    }

    @Override
    public Optional<Bucket> getProxy(K key) {
        return getProxyConfiguration(key)
                .map(configuration -> GridBucket.createLazyBucket(key, () -> configuration, gridProxy));
    }

    @Override
    public Optional<BucketConfiguration> getProxyConfiguration(K key) {
        return gridProxy.getConfiguration(key);
    }

}
