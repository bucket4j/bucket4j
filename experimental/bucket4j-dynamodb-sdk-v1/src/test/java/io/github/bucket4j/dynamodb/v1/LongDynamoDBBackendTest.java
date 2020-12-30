package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

public class LongDynamoDBBackendTest extends BaseDynamoDBBackendTest<Long> {
    @Override
    protected BaseDynamoDBBackend<Long> create(AmazonDynamoDB db, String table, ClientSideConfig config) {
        return DynamoDBBackend.longKey(db, table, config);
    }

    @Override
    protected Long key() {
        return 42L;
    }
}
