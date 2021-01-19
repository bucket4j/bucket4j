package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class NumberDynamoDBTransactionTest extends BaseDynamoDBTransactionTest<Number> {
    @Override
    protected ScalarAttributeType keyType() {
        return ScalarAttributeType.N;
    }

    @Override
    protected Number key() {
        return 42;
    }

    @Override
    protected BaseDynamoDBTransaction transaction(Number key) {
        return new NumberDynamoDBTransaction(db, table, key);
    }
}
