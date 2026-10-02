package io.github.bucket4j.mongodb_async;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testcontainers.mongodb.MongoDBContainer;

import java.nio.charset.StandardCharsets;
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
        String modifiedStateFieldName = "state" + UUID.randomUUID();

        // The bucket4j MongoDB async ProxyManager (MongoDBAsyncCompareAndSwapBasedProxyManager) identifies documents
        // by "_id" = keyMapper.toBytes(key) (Mapper.STRING -> UTF-8 bytes of the key) and stores the raw serialized
        // state bytes as a Binary value under the configured state field name (default "state"), so the helper
        // below must read/write that exact field via the native reactive-streams driver, blocking on the result,
        // bypassing the bucket4j serialization layer.
        BackwardCompatibilityStateCheckHelper<String> basicBackwardCompatibilityHelper =
                createBackwardCompatibilityHelper(basicCollection, "state");
        BackwardCompatibilityStateCheckHelper<String> modifiedBackwardCompatibilityHelper =
                createBackwardCompatibilityHelper(modifiedCollection, modifiedStateFieldName);

        specs = List.of(
                new ProxyManagerSpec<>(
                        "BasicMongoDBCompareAndSwapBasedProxyManager",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jMongoDBAsync.compareAndSwapBasedBuilder(basicCollection)
                ).checkExpiration().checkStateBackwardCompatibility(basicBackwardCompatibilityHelper).withoutBackwardCompatibilityRequestChecker(),
                new ProxyManagerSpec<>(
                        "MongoDBCompareAndSwapBasedProxyManagerWithRenamedFields",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jMongoDBAsync
                                .compareAndSwapBasedBuilder(modifiedCollection)
                                .expiresAtField(modifiedExpiresAtFieldName)
                                .stateField(modifiedStateFieldName)
                ).checkExpiration().checkStateBackwardCompatibility(modifiedBackwardCompatibilityHelper).withoutBackwardCompatibilityRequestChecker()
        );
    }

    private static BackwardCompatibilityStateCheckHelper<String> createBackwardCompatibilityHelper(MongoCollection<Document> collection, String stateFieldName) {
        return new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                byte[] idBytes = key.getBytes(StandardCharsets.UTF_8);
                CompletableFuture<Document> future = new CompletableFuture<>();
                collection.find(Filters.eq("_id", idBytes)).first().subscribe(new Subscriber<>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(Document document) {
                        future.complete(document);
                    }

                    @Override
                    public void onError(Throwable t) {
                        future.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        if (!future.isDone()) {
                            future.complete(null);
                        }
                    }
                });
                Document document = future.join();
                if (document == null) {
                    return null;
                }
                Binary binary = document.get(stateFieldName, Binary.class);
                return binary == null ? null : binary.getData();
            }

            @Override
            public void setRawState(String key, byte[] state) {
                byte[] idBytes = key.getBytes(StandardCharsets.UTF_8);
                CompletableFuture<Void> future = new CompletableFuture<>();
                collection.updateOne(Filters.eq("_id", idBytes), Updates.set(stateFieldName, state)).subscribe(new Subscriber<>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(com.mongodb.client.result.UpdateResult updateResult) {
                        future.complete(null);
                    }

                    @Override
                    public void onError(Throwable t) {
                        future.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        if (!future.isDone()) {
                            future.complete(null);
                        }
                    }
                });
                future.join();
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
