package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import io.github.bucket4j.distributed.proxy.Backend;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.After;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;

public class StringDynamoDBBackendTckTest extends AbstractDistributedBucketTest {
    private static final AmazonDynamoDB db = DynamoDBEmbedded.create().amazonDynamoDB();
    private static final String table = "buckets";

    @Before
    public void createStateTable() {
        Utils.createStateTable(db, table, ScalarAttributeType.S);
    }

    @After
    public void dropStateTable() {
        db.deleteTable(table);
    }

    @Override
    protected Backend<String> getBackend() {
        return DynamoDBBackend.stringKey(db, table, ClientSideConfig.getDefault());
    }

    @Override
    protected void removeBucketFromBackingStorage(String key) {
        Map<String, AttributeValue> attrs = new HashMap<>();
        attrs.put(Constants.Attrs.DEFAULT_KEY_NAME, new AttributeValue().withS(key));

        db.deleteItem(table, attrs);
    }
}
