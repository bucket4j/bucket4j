package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

public class StringDynamoDBBackendTest extends BaseDynamoDBBackendTest<String> {
    @Override
    protected BaseDynamoDBBackend<String> create(AmazonDynamoDB db, String table, ClientSideConfig config) {
        return DynamoDBBackend.stringKey(db, table, config);
    }

    @Override
    protected String key() {
        return "api:read";
    }
}
