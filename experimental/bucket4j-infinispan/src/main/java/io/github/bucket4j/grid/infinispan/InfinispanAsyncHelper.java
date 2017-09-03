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

import org.infinispan.distribution.DistributionManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.function.TriConsumer;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.processor.EntryProcessor;
import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class InfinispanAsyncHelper {

    public static <K, V, R> CompletableFuture<R> invokeAsync(K key, Cache<K, V> cache, EntryProcessor<K, V, R> entryProcessor) {
        org.infinispan.Cache nativeCache = cache.unwrap(org.infinispan.Cache.class);
        DistributionManager distributionManager = nativeCache
                .getAdvancedCache()
                .getDistributionManager();

        Address primaryNodeForKey;
        if (distributionManager == null) {
            // when cluster consists from one node
            Address thisNodeAdress = nativeCache.getCacheManager().getAddress();
            primaryNodeForKey = thisNodeAdress;
        } else {
            primaryNodeForKey = distributionManager.getCacheTopology().getDistribution(key).primary();
        }

        String cacheName = nativeCache.getName();
        Function<EmbeddedCacheManager, R> callable = new InfinispanAsyncHelper.SerializableRemoteFunction<>(key, cacheName, entryProcessor);
        CompletableFuture<R> future = new CompletableFuture<>();
        ResultConsumer<R> resultConsumer = new ResultConsumer<>(future);
        nativeCache.getCacheManager().executor()
                .filterTargets(Collections.singleton(primaryNodeForKey))
                .submitConsumer(callable, resultConsumer);
        return future;
    }

    public static class SerializableRemoteFunction<K, V, R> implements Serializable, Function<EmbeddedCacheManager, R> {

        private final K key;
        private final String cacheName;
        private final EntryProcessor<K, V, R> entryProcessor;

        public SerializableRemoteFunction(K key, String cacheName, EntryProcessor<K, V, R> entryProcessor) {
            this.key = key;
            this.cacheName = cacheName;
            this.entryProcessor = entryProcessor;
        }

        @Override
        public R apply(EmbeddedCacheManager embeddedCacheManager) {
            Cache<K, V> cache = Caching.getCachingProvider().getCacheManager().getCache(cacheName);
            return cache.invoke(key, entryProcessor);
        }

    }

    public static class ResultConsumer<R> implements TriConsumer<Address, R, Throwable> {

        private final CompletableFuture<R> future;

        public ResultConsumer(CompletableFuture<R> future) {
            this.future = future;
        }

        @Override
        public void accept(Address address, R result, Throwable throwable) {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                future.complete(result);
            }
        }
    }

}
