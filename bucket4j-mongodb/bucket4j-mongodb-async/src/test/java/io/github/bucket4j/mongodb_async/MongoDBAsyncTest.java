package io.github.bucket4j.mongodb_async;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testcontainers.containers.MongoDBContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class MongoDBAsyncTest extends AbstractDistributedBucketTest {
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
                        () -> Bucket4jMongoDBAsync.compareAndSwapBasedBuilder(basicCollection)
                ).checkExpiration(),
                new ProxyManagerSpec<>(
                        "MongoDBCompareAndSwapBasedProxyManagerWithRenamedFields",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jMongoDBAsync
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
            CompletableFuture<Void> future = new CompletableFuture<>();
            collection.createIndex(
                    new Document(expiresAtFieldName, 1),
                    new com.mongodb.client.model.IndexOptions().expireAfter(0L, java.util.concurrent.TimeUnit.SECONDS)
            ).subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(1);
                }

                @Override
                public void onNext(String s) {
                    future.complete(null);
                }

                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    future.complete(null);
                }
            });
            future.join();
        }

        return collection;
    }
}
