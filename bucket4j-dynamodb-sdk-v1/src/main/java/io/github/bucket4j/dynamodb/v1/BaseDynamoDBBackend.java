package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedBackend;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapBasedTransaction;

import java.util.Objects;

/**
 * @param <K> type of key. {@code K} is unbound while DynamoDB supports only {@code String (S)},
 *            {@code Number (N)} or {@code Binary (B)} types for primary keys.
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.CoreComponents.html#HowItWorks.CoreComponents.PrimaryKey">
 * DynamoDB documentation for primary key.</a>
 */
abstract class BaseDynamoDBBackend<K> extends AbstractCompareAndSwapBasedBackend<K> {
    protected final AmazonDynamoDB db;
    protected final String table;

    protected BaseDynamoDBBackend(AmazonDynamoDB db, String table, ClientSideConfig config) {
        super(Objects.requireNonNull(config, "config is null"));
        this.db = Objects.requireNonNull(db, "DynamoDB is null");
        this.table = Objects.requireNonNull(table, "table name is null");
    }

    // NOTE: override exists only to expose method to tests.
    @Override
    protected abstract CompareAndSwapBasedTransaction allocateTransaction(K key);

    @Override
    protected final void releaseTransaction(CompareAndSwapBasedTransaction transaction) {
        // no-op
    }
}
