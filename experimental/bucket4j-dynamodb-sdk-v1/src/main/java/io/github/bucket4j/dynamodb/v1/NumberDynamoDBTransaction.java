package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Objects;

/**
 * {@link BaseDynamoDBTransaction} implementation that maps {@link Number}
 * to DynamoDB's {@code N} type.
 */
final class NumberDynamoDBTransaction extends BaseDynamoDBTransaction {
    private final Number key;

    public NumberDynamoDBTransaction(AmazonDynamoDB db, String table, Number key) {
        super(db, table);
        this.key = Objects.requireNonNull(key, "key is null");
    }

    @Override
    protected AttributeValue getKeyAttributeValue() {
        return new AttributeValue().withN(key.toString());
    }
}
