package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

import static org.junit.Assert.*;

public abstract class BaseDynamoDBProxyManagerTest<K> {
    private static final AmazonDynamoDB db = DynamoDBEmbedded.create().amazonDynamoDB();
    private static final String table = "buckets";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Before
    public void createStateTable() {
        Utils.createStateTable(db, table, keyType());
    }

    protected abstract ScalarAttributeType keyType();

    @After
    public void tearDown() {
        db.deleteTable(table);
    }

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
        BaseDynamoDBProxyManager<K> proxyManager = create(db, table, ClientSideConfig.getDefault());
        assertNotNull(proxyManager.beginCompareAndSwapOperation(key()));
    }

    @Test
    public void testBucketRemoval() {
        K key = createRandomKey();
        BaseDynamoDBProxyManager<K> proxyManager = create(db, table, ClientSideConfig.getDefault());

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(4, Duration.ofHours(1)))
                .build();
        BucketProxy bucket = proxyManager.builder().buildProxy(key, configuration);
        bucket.getAvailableTokens();

        assertTrue(proxyManager.getProxyConfiguration(key).isPresent());
        proxyManager.removeProxy(key);
        assertFalse(proxyManager.getProxyConfiguration(key).isPresent());
    }

    /**
     * @param db {@link AmazonDynamoDB} to use as proxyManager.
     * @param table name of DynamoDB table.
     * @param config {@link ClientSideConfig} config to use.
     * @return {@link BaseDynamoDBProxyManager} implementation.
     */
    protected abstract BaseDynamoDBProxyManager<K> create(AmazonDynamoDB db, String table, ClientSideConfig config);

    protected abstract K createRandomKey();

    /**
     * @return key to use for test. Each invocation <i>may</i> return new key. It is
     * up to invoker to store returned value.
     */
    protected abstract K key();
}
