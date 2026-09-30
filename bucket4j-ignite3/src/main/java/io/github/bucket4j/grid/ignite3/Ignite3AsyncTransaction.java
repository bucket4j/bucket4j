package io.github.bucket4j.grid.ignite3;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.apache.ignite.Ignite;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
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
        KeyValueView<K, byte[]> keyValueView = (KeyValueView<K, byte[]>) table.keyValueView(key.getClass(), byte[].class);
        return keyValueView.getAsync(tx, key).thenCompose(((byte[] stateBytes) -> {
            BinaryBatchRequest batch = new BinaryBatchRequest(requests, stateBytes);
            BinaryBatchResults binaryBatchResults = batch.execute();
            if (!binaryBatchResults.stateModified) {
                return CompletableFuture.completedFuture(binaryBatchResults.results);
            } else {
                return keyValueView.putAsync(tx, key, binaryBatchResults.finalState).thenApply((Void v) -> binaryBatchResults.results);
            }
        }));
    }

}
