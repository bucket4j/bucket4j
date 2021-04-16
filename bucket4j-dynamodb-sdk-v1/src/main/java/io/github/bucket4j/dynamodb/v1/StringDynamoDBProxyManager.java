package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * {@link BaseDynamoDBTransaction} implementation that uses {@link String} as key.
 */
final class StringDynamoDBProxyManager extends BaseDynamoDBProxyManager<String> {
    StringDynamoDBProxyManager(AmazonDynamoDB db, String table, ClientSideConfig clientSideConfig) {
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
    public void removeProxy(String key) {
        Map<String, AttributeValue> attrs = new HashMap<>();
        attrs.put(Constants.Attrs.DEFAULT_KEY_NAME, new AttributeValue().withS(key));

        db.deleteItem(table, attrs);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    @Override
    protected CompletableFuture<Void> removeAsync(String key) {
        throw new UnsupportedOperationException();
    }

}
