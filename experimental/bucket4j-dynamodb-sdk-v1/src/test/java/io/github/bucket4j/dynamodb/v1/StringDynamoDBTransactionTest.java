package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;

public class StringDynamoDBTransactionTest extends BaseDynamoDBTransactionTest<String> {
    @Override
    protected ScalarAttributeType keyType() {
        return ScalarAttributeType.S;
    }

    @Override
    protected String key() {
        return "api:read";
    }

    @Override
    protected BaseDynamoDBTransaction transaction(String key) {
        return new StringDynamoDBTransaction(db, table, key);
    }

    @Test
    public void ctorThrowsIfKeyIsNull() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("key is null");
        
        transaction(null);
    }

    @Test
    public void ctorThrowsIfKeyIsEmpty() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("key is empty");

        transaction("");
    }

    @Test
    public void ctorThrowsIfKeyLengthIsOver2048Bytes() {
        String key = repeat("long-key", 500);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("key " + key + " has length of 4000 bytes while max allowed is 2048 bytes");

        transaction(key);
    }

    @Test
    public void ctorAcceptsKeyOf2048Bytes() {
        String key = repeat("a", 2048);

        assertNotNull(transaction(key));
    }

    // Poor man String#repeat
    private static String repeat(String s, int times) {
        return String.join("", Collections.nCopies(times, s));
    }
}
