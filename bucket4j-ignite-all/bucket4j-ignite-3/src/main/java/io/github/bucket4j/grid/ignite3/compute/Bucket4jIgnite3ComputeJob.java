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
package io.github.bucket4j.grid.ignite3.compute;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.ignite.Ignite;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Tuple;

import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

/**
 * Colocated compute job that performs a single get-modify-put cycle for one bucket, executed on the
 * cluster node that owns the bucket's partition.
 *
 * <p>
 * This is intentionally a simple, non-batched, EntryProcessor-style job: it handles exactly one
 * {@link io.github.bucket4j.distributed.remote.Request} per invocation. Request coalescing/batching
 * across concurrent callers is out of scope here and can be layered on top of this job later.
 *
 * <p>
 * Concurrent invocations that target the same bucket key are reconciled with an optimistic
 * compare-and-swap loop on the row's state column, the same primitive used by
 * {@link io.github.bucket4j.grid.ignite3.cas.IgniteKeyValueCasBasedProxyManager}. A plain Ignite
 * transaction wrapping a get and a put is not equivalent to this: a transactional read here does not
 * by itself get treated as a write-conflict trigger, which can let two concurrent transactions both read
 * the same original state and both commit, silently losing one of the updates.
 *
 * <p>
 * This class (and bucket4j-core, which it depends on) must be reachable from the classloader of the
 * Ignite server node that executes it - either by placing the jars on the node's classpath, or by
 * referencing them as Ignite Deployment Units.
 */
public class Bucket4jIgnite3ComputeJob implements ComputeJob<byte[], byte[]> {

    private static final int MAX_ATTEMPTS = 100;

    @Override
    public CompletableFuture<byte[]> executeAsync(JobExecutionContext context, byte[] argument) {
        Bucket4jIgnite3ComputeArgument request = Bucket4jIgnite3ComputeArgument.decode(argument);
        Ignite ignite = context.ignite();
        KeyValueView<Tuple, Tuple> keyValueView = ignite.tables().table(request.tableName).keyValueView();
        Tuple key = Tuple.create().set(request.idColumnName, request.key);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Tuple persistedRow = keyValueView.get(null, key);
            byte[] originalStateBytes = persistedRow == null ? null : persistedRow.bytesValue(request.stateColumnName);
            byte[][] newStateBytesHolder = new byte[1][];

            byte[] resultBytes = new AbstractBinaryTransaction(request.requestBytes) {
                @Override
                public boolean exists() {
                    return persistedRow != null;
                }
                @Override
                protected byte[] getRawState() {
                    return originalStateBytes;
                }
                @Override
                protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
                    newStateBytesHolder[0] = newStateBytes;
                }
            }.execute();

            byte[] newStateBytes = newStateBytesHolder[0];
            if (newStateBytes == null) {
                // command did not modify the bucket state (e.g. a read-only probe), nothing to persist
                return CompletableFuture.completedFuture(resultBytes);
            }

            Tuple newRow = Tuple.create().set(request.stateColumnName, newStateBytes);
            boolean committed = originalStateBytes == null
                ? keyValueView.putIfAbsent(null, key, newRow)
                : keyValueView.replace(null, key, Tuple.create().set(request.stateColumnName, originalStateBytes), newRow);

            if (committed) {
                return CompletableFuture.completedFuture(resultBytes);
            }
            backoff(attempt);
        }
        throw new IllegalStateException("Failed to persist bucket state for key '" + request.key + "' after " + MAX_ATTEMPTS + " compare-and-swap attempts");
    }

    private static void backoff(int attempt) {
        int cap = Math.min(1 + attempt / 5, 25);
        try {
            Thread.sleep(1 + ThreadLocalRandom.current().nextInt(cap));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
