
package io.github.bucket4j.grid.hazelcast;


import com.hazelcast.map.IMap;
import io.github.bucket4j.Extension;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * The extension of Bucket4j library addressed to support <a href="https://hazelcast.com//">Hazelcast</a> in-memory data grid.
 *
 * Use this extension only if you need in asynchronous API, else stay at {@link io.github.bucket4j.grid.jcache.JCache}
 */
public class Hazelcast implements Extension<HazelcastBucketBuilder> {

    /**
     * {@inheritDoc}
     *
     * @return new instance of {@link HazelcastBucketBuilder}
     */
    @Override
    public HazelcastBucketBuilder builder() {
        return new HazelcastBucketBuilder();
    }

    /**
     * Creates {@link HazelcastProxyManager} for specified map.
     *
     * @param map map for storing state of buckets
     * @param <T> type of keys in the map
     * @return {@link ProxyManager} for specified map.
     */
    public <T extends Serializable> ProxyManager<T> proxyManagerForMap(IMap<T, GridBucketState> map) {
        return new HazelcastProxyManager<>(map);
    }

    @Override
    public Collection<SerializationHandle<?>> getSerializers() {
        return Arrays.asList(SimpleBackupProcessor.SERIALIZATION_HANDLE);
    }

}
