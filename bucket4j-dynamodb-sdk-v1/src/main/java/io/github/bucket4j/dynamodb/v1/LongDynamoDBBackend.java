package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;

/**
 * {@link BaseDynamoDBBackend} implementation that uses {@link Long} as key.
 */
final class LongDynamoDBBackend extends BaseDynamoDBBackend<Long> {
    LongDynamoDBBackend(AmazonDynamoDB db, String table, ClientSideConfig config) {
        super(db, table, config);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(Long key) {
        return new NumberDynamoDBTransaction(db, table, key);
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(Long key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }
}
