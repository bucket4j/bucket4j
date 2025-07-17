package io.github.bucket4j.mongodb;

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

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class MongoDBTest extends AbstractDistributedBucketTest {
    private static MongoDBContainer mongoDBContainer;
    private static MongoClient mongoClient;

    @BeforeAll
    public static void setupMongo() {
        mongoDBContainer = new MongoDBContainer("mongo:latest");
        mongoDBContainer.start();
        mongoDBContainer.followOutput(outputFrame -> System.out.print(outputFrame.getUtf8String()));

        mongoClient = MongoClients.create(mongoDBContainer.getConnectionString());
        MongoDatabase mongoDatabase = mongoClient.getDatabase("bucket4j_test");
        MongoCollection<Document> collection = mongoDatabase.getCollection("bucket");

        CompletableFuture<Void> future = new CompletableFuture<>();
        collection.createIndex(
                new Document("expiresAt", 1),
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

        specs = Collections.singletonList(
                new ProxyManagerSpec<>(
                        "MongoDBCompareAndSwapBasedProxyManager",
                        () -> UUID.randomUUID().toString(),
                        () -> Bucket4jMongoDB.compareAndSwapBasedBuilder(collection)
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
}
