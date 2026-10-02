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
package io.github.bucket4j.grid.ignite3;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.apache.ignite.Ignite;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;

import io.github.bucket4j.distributed.remote.BinaryBatchRequest;
import io.github.bucket4j.distributed.remote.BinaryBatchResults;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;

/**
 * The body of the Ignite transaction that applies a batch of bucket4j requests to the state of a single bucket.
 *
 * <p>Reads the row identified by {@link Ignite3ProxyManager#KEY_COLUMN_NAME}, executes all requests of the batch
 * against the state stored in {@link Ignite3ProxyManager#STATE_COLUMN_NAME} (a missing row is treated as a missing
 * bucket), and writes the new state back only if at least one request has modified it. The table is accessed
 * via {@link Tuple} views, so columns other than the two above are never touched.
 *
 * @param <K> the type of bucket key
 */
public class Ignite3AsyncTransaction<K> implements Function<Transaction, CompletableFuture<List<CommandResult<?>>>> {

    private final Ignite ignite;
    private final String tableName;
    private final K key;
    private final List<Request<?>> requests;

    /**
     * @param ignite the Ignite instance of the node that executes the job
     * @param tableName the name of the table that holds bucket state
     * @param key the key of the bucket
     * @param requests the requests to execute, in the order in which their results are returned
     */
    public Ignite3AsyncTransaction(Ignite ignite, String tableName, K key, List<Request<?>> requests) {
        this.ignite = ignite;
        this.tableName = tableName;
        this.key = key;
        this.requests = requests;
    }

    /**
     * @return the future that is completed with one result per request, in the same order as the requests
     */
    @Override
    public CompletableFuture<List<CommandResult<?>>> apply(Transaction tx) {
        Table table =  ignite.tables().table(tableName);

        KeyValueView<Tuple, Tuple> keyValueView = table.keyValueView();
        Tuple keyTuple = Tuple.create().set(Ignite3ProxyManager.KEY_COLUMN_NAME, key);

        return keyValueView.getAsync(tx, keyTuple).thenCompose(((Tuple persistedTuple) -> {
            byte[] stateBytes = persistedTuple == null ? null : persistedTuple.bytesValue(Ignite3ProxyManager.STATE_COLUMN_NAME);
            BinaryBatchRequest batch = new BinaryBatchRequest(requests, stateBytes);
            BinaryBatchResults binaryBatchResults = batch.execute();
            if (!binaryBatchResults.stateModified) {
                return CompletableFuture.completedFuture(binaryBatchResults.results);
            } else {
                Tuple newValue = Tuple.create().set(Ignite3ProxyManager.STATE_COLUMN_NAME, binaryBatchResults.newStateBytes);
                return keyValueView.putAsync(tx, keyTuple, newValue).thenApply((Void v) -> binaryBatchResults.results);
            }
        }));
    }

}
