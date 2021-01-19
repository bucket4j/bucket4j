package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapBasedTransaction;

/**
 * {@link BaseDynamoDBTransaction} implementation that uses {@link String} as key.
 */
final class StringDynamoDBBackend extends BaseDynamoDBBackend<String> {
    StringDynamoDBBackend(AmazonDynamoDB db, String table, ClientSideConfig clientSideConfig) {
        super(db, table, clientSideConfig);
    }

    @Override
    protected CompareAndSwapBasedTransaction allocateTransaction(String key) {
        return new StringDynamoDBTransaction(db, table, key);
    }
}
