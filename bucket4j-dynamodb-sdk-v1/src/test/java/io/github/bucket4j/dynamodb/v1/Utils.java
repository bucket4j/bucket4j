package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

import java.util.Collections;
import java.util.List;

final class Utils {
    public static void createStateTable(AmazonDynamoDB db, String table, ScalarAttributeType keyType) {

        List<KeySchemaElement> schema = Collections.singletonList(
                new KeySchemaElement(Constants.Attrs.DEFAULT_KEY_NAME, KeyType.HASH)
        );
        List<AttributeDefinition> attrs = Collections.singletonList(
                new AttributeDefinition(Constants.Attrs.DEFAULT_KEY_NAME, keyType)
        );

        CreateTableRequest request = new CreateTableRequest()
                .withTableName(table)
                .withKeySchema(schema)
                .withAttributeDefinitions(attrs)
                .withBillingMode(BillingMode.PAY_PER_REQUEST);

        db.createTable(request);
    }

    private Utils() {}
}
