/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2026 Vladimir Bukhtoyarov
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
package io.github.bucket4j.util.concurrent.batch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Registry of independent {@link AsyncBatchHelper} instances, partitioned by an arbitrary key.
 *
 * <p>A batcher for a given key is created lazily via the supplied factory on first use, shared
 * by all concurrent callers targeting the same key, and evicted once there are no requests in
 * flight for it - so the registry does not grow unbounded over the application lifetime.
 *
 * @param <K> batch key type
 * @param <T> task type
 * @param <R> task result type
 * @param <CT> combined task type
 * @param <CR> combined task result type
 */
public class MultiAsyncBatcherHelper<K, T, R, CT, CR> {

    private final ConcurrentHashMap<K, BatcherEntry<T, R, CT, CR>> batchersByKey = new ConcurrentHashMap<>();

    public CompletableFuture<R> executeAsync(K key, T task, Function<K, AsyncBatchHelper<T, R, CT, CR>> batcherFactory) {
        BatcherEntry<T, R, CT, CR> entry = batchersByKey.compute(key, (k, previous) -> {
            if (previous != null) {
                previous.inProgressCount++;
                return previous;
            } else {
                return new BatcherEntry<>(batcherFactory.apply(key));
            }
        });

        AtomicBoolean released = new AtomicBoolean();
        try {
            return entry.batcher.executeAsync(task)
                    .whenComplete((result, error) -> release(key, released));
        } catch (Throwable t) {
            release(key, released);
            throw t;
        }
    }

    private void release(K key, AtomicBoolean released) {
        if (released.compareAndSet(false, true)) {
            batchersByKey.compute(key, (k, previous) -> {
                if (previous == null) {
                    return null;
                }
                previous.inProgressCount--;
                return previous.inProgressCount == 0 ? null : previous;
            });
        }
    }

    private static final class BatcherEntry<T, R, CT, CR> {
        private int inProgressCount = 1;
        private final AsyncBatchHelper<T, R, CT, CR> batcher;

        BatcherEntry(AsyncBatchHelper<T, R, CT, CR> batcher) {
            this.batcher = batcher;
        }
    }

}
