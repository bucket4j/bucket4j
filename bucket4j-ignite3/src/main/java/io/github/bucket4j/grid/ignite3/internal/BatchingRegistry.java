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
package io.github.bucket4j.grid.ignite3.internal;

import io.github.bucket4j.util.concurrent.batch.BatchHelper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Maintains one {@link BatchHelper} per bucket key, so that concurrent client calls for the same key are coalesced
 * into a single remote compute job invocation, while calls for different keys do not contend with each other.
 *
 * <p>Entries are removed as soon as their last concurrent caller is done with them. Ref-counting is used instead of,
 * say, a size-bounded cache, because {@link BatchHelper} exposes no way to check whether its internal queue is
 * empty, so there is no other race-free way to know when it is safe to drop an entry.
 */
public final class BatchingRegistry<K> {

    private final ConcurrentHashMap<K, Entry> entriesByKey = new ConcurrentHashMap<>();
    private final BiFunction<K, List<byte[]>, byte[]> jobExecutor;

    /**
     * @param jobExecutor takes the bucket key and the coalesced list of serialized requests for that key,
     *                    submits a single remote job for the whole batch, and returns the raw job output bytes
     *                    (an {@link ByteArrayListCodec}-encoded list of serialized results, one per request).
     */
    public BatchingRegistry(BiFunction<K, List<byte[]>, byte[]> jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    public byte[] execute(K key, byte[] requestBytes) {
        Entry entry = acquire(key);
        try {
            return entry.batchHelper.execute(requestBytes);
        } finally {
            release(key);
        }
    }

    private Entry acquire(K key) {
        return entriesByKey.compute(key, (k, existing) -> {
            if (existing == null) {
                existing = new Entry(newBatchHelper(k));
            }
            existing.refCount++;
            return existing;
        });
    }

    private void release(K key) {
        entriesByKey.compute(key, (k, existing) -> {
            existing.refCount--;
            return existing.refCount == 0 ? null : existing;
        });
    }

    private BatchHelper<byte[], byte[], List<byte[]>, byte[]> newBatchHelper(K key) {
        return BatchHelper.create(
                Function.identity(),
                combinedTask -> jobExecutor.apply(key, combinedTask),
                (combinedTask, combinedResult) -> ByteArrayListCodec.decode(combinedResult)
        );
    }

    private static final class Entry {
        final BatchHelper<byte[], byte[], List<byte[]>, byte[]> batchHelper;
        int refCount;

        Entry(BatchHelper<byte[], byte[], List<byte[]>, byte[]> batchHelper) {
            this.batchHelper = batchHelper;
        }
    }

}
