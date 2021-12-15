package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import java.util.concurrent.ThreadLocalRandom;

public class LongDynamoDBProxyManagerTest extends BaseDynamoDBProxyManagerTest<Long> {
    @Override
    protected BaseDynamoDBProxyManager<Long> create(AmazonDynamoDB db, String table, ClientSideConfig config) {
        return DynamoDBProxyManager.longKey(db, table, config);
    }

    @Override
    protected Long createRandomKey() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Override
    protected Long key() {
        return 42L;
    }

    @Override
    protected ScalarAttributeType keyType() {
        return ScalarAttributeType.N;
    }
}
