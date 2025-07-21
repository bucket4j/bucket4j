import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.bucket4j.mongodb_sync.Bucket4jMongoDBSync;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MongoDBContainer;

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

        specs = List.of(
                new ProxyManagerSpec<>(
                        "BasicMongoDBCompareAndSwapBasedProxyManager",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jMongoDBSync.compareAndSwapBasedBuilder(basicCollection)
                ).checkExpiration(),
                new ProxyManagerSpec<>(
                        "MongoDBCompareAndSwapBasedProxyManagerWithRenamedFields",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jMongoDBSync
                                .compareAndSwapBasedBuilder(modifiedCollection)
                                .expiresAtField(modifiedExpiresAtFieldName)
                                .stateField("state" + UUID.randomUUID())
                ).checkExpiration()
        );
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
