package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseDynamoDBProxyManagerTest<K> {
    private static final AmazonDynamoDB db = DynamoDBEmbedded.create().amazonDynamoDB();
    private static final String table = "buckets";

    @BeforeEach
    public void createStateTable() {
        Utils.createStateTable(db, table, keyType());
    }

    protected abstract ScalarAttributeType keyType();

    @AfterEach
    public void tearDown() {
        db.deleteTable(table);
    }

    @Test
    public void ctorThrowsIfDynamoDBIsNull() {
        Exception e = assertThrows(NullPointerException.class, () -> create(null, table, ClientSideConfig.getDefault()));
        assertEquals("DynamoDB is null", e.getMessage());
    }

    @Test
    public void ctorThrowsIfTableNameIsNull() {
        Exception e = assertThrows(NullPointerException.class, () -> create(db, null, ClientSideConfig.getDefault()));
        assertEquals("table name is null", e.getMessage());
    }

    @Test
    public void ctorThrowsIfConfigIsNull() {
        Exception e = assertThrows(NullPointerException.class, () -> create(db, table, null));
        assertEquals("config is null", e.getMessage());
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
        BucketProxy bucket = proxyManager.builder().build(key, configuration);
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
