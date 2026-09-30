import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.github.bucket4j.mongodb_sync.Bucket4jMongoDBSync;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.mongodb.MongoDBContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class MongoDBSyncTest extends AbstractDistributedBucketTest {
    private static MongoDBContainer mongoDBContainer;
    private static MongoClient mongoClient;

    @BeforeAll
    public static void setupMongo() {
        mongoDBContainer = new MongoDBContainer("mongo:latest");
        mongoDBContainer.start();

        mongoClient = MongoClients.create(mongoDBContainer.getConnectionString());
        MongoDatabase mongoDatabase = mongoClient.getDatabase("bucket4j_test");

        String basicExpiresAtFieldName = "expiresAt";
        MongoCollection<Document> basicCollection = prepareCollection(mongoDatabase, "bucket", basicExpiresAtFieldName, false);

        String modifiedExpiresAtFieldName = "expiresAt" + UUID.randomUUID();
        MongoCollection<Document> modifiedCollection = prepareCollection(mongoDatabase, "bucket_modified", modifiedExpiresAtFieldName, false);
        String modifiedStateFieldName = "state" + UUID.randomUUID();

        // The bucket4j MongoDB sync ProxyManager (MongoDBSyncCompareAndSwapBasedProxyManager) identifies documents
        // by "_id" = keyMapper.toBytes(key) (Mapper.STRING -> UTF-8 bytes of the key) and stores the raw serialized
        // state bytes as a Binary value under the configured state field name (default "state"), so the helper
        // below must read/write that exact field via the native driver, bypassing the bucket4j serialization layer.
        BackwardCompatibilityStateCheckHelper<String> basicBackwardCompatibilityHelper =
                createBackwardCompatibilityHelper(basicCollection, "state");
        BackwardCompatibilityStateCheckHelper<String> modifiedBackwardCompatibilityHelper =
                createBackwardCompatibilityHelper(modifiedCollection, modifiedStateFieldName);

        specs = List.of(
                new ProxyManagerSpec<>(
                        "BasicMongoDBCompareAndSwapBasedProxyManager",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jMongoDBSync.compareAndSwapBasedBuilder(basicCollection)
                ).checkExpiration().checkStateBackwardCompatibility(basicBackwardCompatibilityHelper),
                new ProxyManagerSpec<>(
                        "MongoDBCompareAndSwapBasedProxyManagerWithRenamedFields",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jMongoDBSync
                                .compareAndSwapBasedBuilder(modifiedCollection)
                                .expiresAtField(modifiedExpiresAtFieldName)
                                .stateField(modifiedStateFieldName)
                ).checkExpiration().checkStateBackwardCompatibility(modifiedBackwardCompatibilityHelper)
        );
    }

    private static BackwardCompatibilityStateCheckHelper<String> createBackwardCompatibilityHelper(MongoCollection<Document> collection, String stateFieldName) {
        return new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                byte[] idBytes = key.getBytes(StandardCharsets.UTF_8);
                Document document = collection.find(Filters.eq("_id", idBytes)).first();
                if (document == null) {
                    return null;
                }
                Binary binary = document.get(stateFieldName, Binary.class);
                return binary == null ? null : binary.getData();
            }

            @Override
            public void setRawState(String key, byte[] state) {
                byte[] idBytes = key.getBytes(StandardCharsets.UTF_8);
                collection.updateOne(Filters.eq("_id", idBytes), Updates.set(stateFieldName, state));
            }
        };
    }

    @AfterAll
    public static void cleanupMongo() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        mongoDBContainer.stop();
    }

    /*
    ttl index always false however long duration tests may use it
     */
    private static MongoCollection<Document> prepareCollection(MongoDatabase mongoDatabase, String collectionName, String expiresAtFieldName, boolean useTtlIndex) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

        if (useTtlIndex) {
            collection.createIndex(
                    new Document(expiresAtFieldName, 1),
                    new com.mongodb.client.model.IndexOptions().expireAfter(0L, java.util.concurrent.TimeUnit.SECONDS)
            );
        }

        return collection;
    }

}
