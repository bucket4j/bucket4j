/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.grid.infinispan.hotrod;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.functional.FunctionalMap.ReadWriteMap;
import org.infinispan.functional.impl.FunctionalMapImpl;
import org.infinispan.functional.impl.ReadWriteMapImpl;
import org.infinispan.tasks.ServerTask;
import org.infinispan.tasks.TaskContext;

import io.github.bucket4j.grid.infinispan.InfinispanProcessor;

public class Bucket4jTask implements ServerTask<byte[]> {

    public static final String TASK_NAME = "bucket4j_readWriteMapBased_task";

    private static final ConcurrentHashMap<String, CacheContext> perCacheContext = new ConcurrentHashMap<>();
    private static final ThreadLocal<TaskContext> threadLocalTaskContext = new ThreadLocal<>();
    public static final String REQUEST_PARAM = "request";
    public static final String KEY_PARAM = "key";

    private static final class CacheContext {

        private final ReadWriteMap<Object, byte[]> readWriteMap;

        private CacheContext(ReadWriteMap<Object, byte[]> readWriteMap) {
            this.readWriteMap = readWriteMap;
        }
    }

    @Override
    public void setTaskContext(TaskContext ctx) {
        Cache<?, byte[]> cache = (Cache<?, byte[]>) ctx.getCache().get();
        AdvancedCache<?, byte[]> advancedCache = cache.getAdvancedCache();
        String cacheName = advancedCache.getName();
        CacheContext cacheContext = perCacheContext.get(cacheName);
        if (cacheContext == null) {
            perCacheContext.computeIfAbsent(cacheName, k -> {
                FunctionalMapImpl<Object, byte[]> functionalMap = FunctionalMapImpl.create((AdvancedCache) advancedCache);
                return new CacheContext(ReadWriteMapImpl.create(functionalMap));
            });
        }
        threadLocalTaskContext.set(ctx);
    }

    @Override
    public byte[] call() throws Exception {
        TaskContext ctx = threadLocalTaskContext.get();
        Cache<Object, byte[]> cache = (Cache<Object, byte[]>) ctx.getCache().get();
        AdvancedCache<?, byte[]> advancedCache = cache.getAdvancedCache();
        String cacheName = advancedCache.getName();
        CacheContext cacheContext = perCacheContext.get(cacheName);
        Map<String, ?> namedParamaters = ctx.getParameters().get();
        byte[] requestBytes = (byte[]) namedParamaters.get(REQUEST_PARAM);
        Object key = namedParamaters.get(KEY_PARAM);

        InfinispanProcessor<Object, byte[]> entryProcessor = new InfinispanProcessor<>(requestBytes);
        try {
            CompletableFuture<byte[]> resultFuture = cacheContext.readWriteMap.eval(key, entryProcessor);
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public String getName() {
        return TASK_NAME;
    }

}
