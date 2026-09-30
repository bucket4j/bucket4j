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

public class Ignite3AsyncTransaction<K> implements Function<Transaction, CompletableFuture<List<CommandResult<?>>>> {

    private final Ignite ignite;
    private final String tableName;
    private final K key;
    private final List<Request<?>> requests;

    public Ignite3AsyncTransaction(Ignite ignite, String tableName, K key, List<Request<?>> requests) {
        this.ignite = ignite;
        this.tableName = tableName;
        this.key = key;
        this.requests = requests;
    }

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
