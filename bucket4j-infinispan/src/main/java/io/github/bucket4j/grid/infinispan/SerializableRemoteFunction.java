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

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.cache.Cache;
import javax.cache.Caching;
import java.io.Serializable;
import java.util.function.Function;

class SerializableRemoteFunction<K extends Serializable, T extends Serializable> implements Function<EmbeddedCacheManager, CommandResult<T>> {

    private final K key;
    private final String cacheName;
    private final JCacheEntryProcessor<K, T> entryProcessor;

    public SerializableRemoteFunction(K key, String cacheName, JCacheEntryProcessor<K, T> entryProcessor) {
        this.key = key;
        this.cacheName = cacheName;
        this.entryProcessor = entryProcessor;
    }

    @Override
    public CommandResult<T> apply(EmbeddedCacheManager embeddedCacheManager) {
        @SuppressWarnings("unchecked")
        Cache<K, GridBucketState> cache = Caching.getCache(cacheName, (Class<K>) key.getClass(), GridBucketState.class);
        return cache.invoke(key, entryProcessor);
    }

}
