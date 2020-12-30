package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapBasedTransaction;

/**
 * {@link BaseDynamoDBBackend} implementation that uses {@link Long} as key.
 */
final class LongDynamoDBBackend extends BaseDynamoDBBackend<Long> {
    LongDynamoDBBackend(AmazonDynamoDB db, String table, ClientSideConfig config) {
        super(db, table, config);
    }

    @Override
    protected CompareAndSwapBasedTransaction allocateTransaction(Long key) {
        return new NumberDynamoDBTransaction(db, table, key);
    }
}
