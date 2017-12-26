/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
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

package io.github.bucket4j.grid.ignite;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridProxy;
import io.github.bucket4j.grid.ProxyManager;
import org.apache.ignite.IgniteCache;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Ignite specific implementation of {@link ProxyManager}
 *
 * @param <K> type of key for buckets
 */
public class IgniteProxyManager<K extends Serializable> implements ProxyManager<K> {

    private final GridProxy<K> gridProxy;

    IgniteProxyManager(IgniteCache<K, GridBucketState> cache) {
        if (cache == null) {
            throw new IllegalArgumentException("cache must not be null");
        }
        this.gridProxy = new IgniteProxy<>(cache);
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
