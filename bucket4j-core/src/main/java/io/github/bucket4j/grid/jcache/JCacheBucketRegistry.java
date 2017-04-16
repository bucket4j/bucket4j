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
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class JCacheBucketRegistry<K extends Serializable> implements BucketRegistry<K> {

    private final GridProxy<K> gridProxy;
    private final Function<K, BucketConfiguration> configurationSupplier;

    public static <T extends Serializable> JCacheBucketRegistry<T> withKeyIndependentConfiguration(Cache<T, GridBucketState> cache, BucketConfiguration configuration) {
        Objects.requireNonNull(configuration);
        Function<T, BucketConfiguration> configurationSupplier = (key) -> configuration;
        return new JCacheBucketRegistry<>(cache, configurationSupplier);
    }

    public static <T extends Serializable> JCacheBucketRegistry<T> withKeyDependentConfiguration(Cache<T, GridBucketState> cache, Function<T, BucketConfiguration> configurationSupplier) {
        return new JCacheBucketRegistry<>(cache, configurationSupplier);
    }

    private JCacheBucketRegistry(Cache<K, GridBucketState> cache, Function<K, BucketConfiguration> configurationSupplier) {
        this.gridProxy = new JCacheProxy<>(cache);
        this.configurationSupplier = configurationSupplier;
    }

    @Override
    public Bucket getProxy(K key) {
        Supplier<BucketConfiguration> lazyConfigurationSupplier = new LazySupplier<>(() -> configurationSupplier.apply(key));
        return GridBucket.createLazyBucket(key, lazyConfigurationSupplier, gridProxy);
    }

}
