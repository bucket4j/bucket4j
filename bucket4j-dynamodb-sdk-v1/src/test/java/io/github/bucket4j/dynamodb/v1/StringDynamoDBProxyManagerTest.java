package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import java.util.UUID;

public class StringDynamoDBProxyManagerTest extends BaseDynamoDBProxyManagerTest<String> {
    @Override
    protected BaseDynamoDBProxyManager<String> create(AmazonDynamoDB db, String table, ClientSideConfig config) {
        return DynamoDBProxyManager.stringKey(db, table, config);
    }

    @Override
    protected ScalarAttributeType keyType() {
        return ScalarAttributeType.S;
    }

    @Override
    protected String createRandomKey() {
        return UUID.randomUUID().toString();
    }

    @Override
    protected String key() {
        return "api:read";
    }
}
