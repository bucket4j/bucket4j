package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;

public abstract class BaseDynamoDBBackendTest<K> {
    private static final AmazonDynamoDB db = DynamoDBEmbedded.create().amazonDynamoDB();
    private static final String table = "buckets";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void ctorThrowsIfDynamoDBIsNull() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("DynamoDB is null");

        create(null, table, ClientSideConfig.getDefault());
    }

    @Test
    public void ctorThrowsIfTableNameIsNull() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("table name is null");

        create(db, null, ClientSideConfig.getDefault());
    }

    @Test
    public void ctorThrowsIfConfigIsNull() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("config is null");

        create(db, table, null);
    }

    @Test
    public void allocateTransactionReturnsNonNullTransaction() {
        BaseDynamoDBBackend<K> backend = create(db, table, ClientSideConfig.getDefault());

        assertNotNull(backend.allocateTransaction(key()));
    }

    /**
     * @param db {@link AmazonDynamoDB} to use as backend.
     * @param table name of DynamoDB table.
     * @param config {@link ClientSideConfig} config to use.
     * @return {@link BaseDynamoDBBackend} implementation.
     */
    protected abstract BaseDynamoDBBackend<K> create(AmazonDynamoDB db, String table, ClientSideConfig config);

    /**
     * @return key to use for test. Each invocation <i>may</i> return new key. It is
     * up to invoker to store returned value.
     */
    protected abstract K key();
}
