package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;

/**
 * {@link BaseDynamoDBTransaction} implementation that uses {@link String} as key.
 */
final class StringDynamoDBBackend extends BaseDynamoDBBackend<String> {
    StringDynamoDBBackend(AmazonDynamoDB db, String table, ClientSideConfig clientSideConfig) {
        super(db, table, clientSideConfig);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(String key) {
        return new StringDynamoDBTransaction(db, table, key);
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }
}
