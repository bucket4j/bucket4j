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

package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.grid.BucketRegistry;
import io.github.bucket4j.grid.GridBucket;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridProxy;
import io.github.bucket4j.util.LazySupplier;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.function.Supplier;

public class JCacheBucketRegistry<K extends Serializable> implements BucketRegistry<K> {

    private final GridProxy<K> gridProxy;

    public static <T extends Serializable> JCacheBucketRegistry<T> forCache(Cache<T, GridBucketState> cache) {
        return new JCacheBucketRegistry<>(cache);
    }

    private JCacheBucketRegistry(Cache<K, GridBucketState> cache) {
        this.gridProxy = new JCacheProxy<>(cache);
    }

    @Override
    public Bucket getProxy(K key, Supplier<BucketConfiguration> configurationLazySupplier) {
        if (!(configurationLazySupplier instanceof LazySupplier)) {
            configurationLazySupplier = new LazySupplier<>(configurationLazySupplier);
        }
        return GridBucket.createLazyBucket(key, configurationLazySupplier, gridProxy);
    }

}
