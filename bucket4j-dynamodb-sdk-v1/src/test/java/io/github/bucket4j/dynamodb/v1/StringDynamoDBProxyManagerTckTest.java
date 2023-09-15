package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

public class StringDynamoDBProxyManagerTckTest extends AbstractDistributedBucketTest<String> {
    private static final AmazonDynamoDB db = DynamoDBEmbedded.create().amazonDynamoDB();
    private static final String table = "buckets";

    @BeforeEach
    public void createStateTable() {
        Utils.createStateTable(db, table, ScalarAttributeType.S);
    }

    @AfterEach
    public void dropStateTable() {
        db.deleteTable(table);
    }

    @Override
    protected ProxyManager<String> getProxyManager() {
        return DynamoDBProxyManager.stringKey(db, table, ClientSideConfig.getDefault());
    }

    @Override
    protected String generateRandomKey() {
        return UUID.randomUUID().toString();
    }

}
