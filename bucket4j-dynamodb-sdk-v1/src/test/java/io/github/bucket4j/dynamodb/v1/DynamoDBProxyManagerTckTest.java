package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DynamoDBProxyManagerTckTest extends AbstractDistributedBucketTest {
    private static final AmazonDynamoDB db = DynamoDBEmbedded.create().amazonDynamoDB();
    private static final String table_long = "buckets_long";
    private static final String table_string = "buckets_string";

    @BeforeAll
    public static void init() {
        Utils.createStateTable(db, table_long, ScalarAttributeType.N);
        Utils.createStateTable(db, table_string, ScalarAttributeType.S);
        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "DynamoDBProxyManager.longKey",
                () -> ThreadLocalRandom.current().nextLong(),
                DynamoDBProxyManager.longKey(db, table_long, ClientSideConfig.getDefault())
            ),
            new ProxyManagerSpec<>(
                "DynamoDBProxyManager.stringKey",
                () -> UUID.randomUUID().toString(),
                DynamoDBProxyManager.stringKey(db, table_string, ClientSideConfig.getDefault())
            )
        );
    }

    @AfterAll
    public static void dropStateTable() {
        db.deleteTable(table_long);
        db.deleteTable(table_string);
    }

}
