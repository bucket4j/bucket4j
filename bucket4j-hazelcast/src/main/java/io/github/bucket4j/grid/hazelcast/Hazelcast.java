/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.grid.hazelcast;


import com.hazelcast.core.IMap;
import io.github.bucket4j.Extension;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import java.io.Serializable;

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

}
