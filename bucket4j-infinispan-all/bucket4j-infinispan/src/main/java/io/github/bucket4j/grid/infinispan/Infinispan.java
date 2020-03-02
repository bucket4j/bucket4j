

package io.github.bucket4j.grid.infinispan;


import io.github.bucket4j.Extension;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;

import io.github.bucket4j.serialization.SerializationHandle;
import org.infinispan.functional.FunctionalMap.ReadWriteMap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 *
 * Use this extension only if you need in asynchronous API, else stay at {@link io.github.bucket4j.grid.jcache.JCache}
 */
public class Infinispan implements Extension<InfinispanBucketBuilder> {

    /**
     * {@inheritDoc}
     *
     * @return new instance of {@link InfinispanBucketBuilder}
     */
    @Override
    public InfinispanBucketBuilder builder() {
        return new InfinispanBucketBuilder();
    }

    /**
     * Creates {@link InfinispanProxyManager} for specified cache.
     *
     * @param readWriteMap cache for storing state of buckets
     * @param <K> type of keys in the cache
     * @return {@link ProxyManager} for specified cache.
     */
    public <K extends Serializable> ProxyManager<K> proxyManagerForMap(ReadWriteMap<K, GridBucketState> readWriteMap) {
        return new InfinispanProxyManager<>(readWriteMap);
    }

    @Override
    public Collection<SerializationHandle<?>> getSerializers() {
        return Arrays.asList(SerializableFunctionAdapter.SERIALIZATION_HANDLE);
    }

}
