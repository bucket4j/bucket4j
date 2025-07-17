package io.github.bucket4j.mongodb;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.expiration.NoneExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.mongodb.Bucket4jMongoDB.MongoDBCompareAndSwapBasedProxyManagerBuilder;
import org.bson.Document;
import org.bson.types.Binary;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

public class MongoDBCompareAndSwapBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {
    private static final Logger logger = Logger.getLogger(MongoDBCompareAndSwapBasedProxyManager.class.getName());
    
    private final MongoCollection<Document> collection;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;


    protected MongoDBCompareAndSwapBasedProxyManager(MongoDBCompareAndSwapBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.collection = builder.getCollection();
        this.keyMapper = builder.getKeyMapper();
        Optional<ExpirationAfterWriteStrategy> expirationStrategyFromClientConfig = builder.getClientSideConfig().getExpirationAfterWriteStrategy();
        this.expirationStrategy = expirationStrategyFromClientConfig.orElseGet(ExpirationAfterWriteStrategy::none);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            private final byte[] keyBytes = keyMapper.toBytes(key);

            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                logger.log(Level.FINE, "CompareAndSwapOperation.getStateData - sync call");
                return getBucketStateFuture(keyBytes).join();
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                logger.log(Level.FINE, "CompareAndSwapOperation.compareAndSwap - sync call");
                return compareAndSwapFuture(keyBytes, originalData, newData, newState).join();
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        return new AsyncCompareAndSwapOperation() {
            private final byte[] keyBytes = keyMapper.toBytes(key);

            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                logger.log(Level.FINE, "AsyncCompareAndSwapOperation.getStateData - async call");
                return getBucketStateFuture(keyBytes);
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                logger.log(Level.FINE, "AsyncCompareAndSwapOperation.compareAndSwap - async call");
                return compareAndSwapFuture(keyBytes, originalData, newData, newState);
            }
        };
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        ByteBuffer keyBytes = ByteBuffer.wrap(keyMapper.toBytes(key));
        logger.log(Level.FINE, "removeAsync - removing document with key");
        CompletableFuture<Void> future = new CompletableFuture<>();

        collection
                .deleteOne(Filters.eq("_id", keyBytes))
                .subscribe(new MongoDBUtilitySubscriber<DeleteResult, Void>(future));

        return future;
    }

    @Override
    public void removeProxy(K key) {
        logger.log(Level.FINE, "removeProxy - synchronously removing proxy for key");
        removeAsync(key).join();
        logger.log(Level.FINE, "removeProxy - proxy removal completed");
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    private CompletableFuture<Optional<byte[]>> getBucketStateFuture(byte[] key) {
        logger.log(Level.FINE, "getBucketStateFuture - looking for document with key length: {0}", key.length);
        CompletableFuture<Document> future = new CompletableFuture<>();

        collection.find(Filters.eq("_id", key))
                .projection(
                        Projections.fields(
                                Projections.include("state"),
                                Projections.excludeId()
                        )
                )
                .first()
                .subscribe(new MongoDBUtilitySubscriber<Document, Document>(future));

        return future.thenApply(bucketState -> {
            if (bucketState == null) {
                logger.log(Level.FINE, "getBucketStateFuture - document not found, returning empty");
                return Optional.empty();
            } else {
                logger.log(Level.FINE, "getBucketStateFuture - document found, extracting state");
                return Optional.ofNullable(bucketState.get("state", Binary.class)).map(Binary::getData);
            }
        });
    }

    private CompletableFuture<Boolean> compareAndSwapFuture(byte[] keyBytes, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        logger.log(Level.FINE, "Starting compareAndSwapFuture - originalData: {0}, newData length: {1}", 
                new Object[]{originalData == null ? "null" : "[" + originalData.length + " bytes]", newData.length});
//        List<Document> results = new ArrayList<>();
//        CountDownLatch latch = new CountDownLatch(1);
//
//        collection.find().subscribe(new Subscriber<Document>() {
//            private Subscription subscription;
//
//            @Override
//            public void onSubscribe(Subscription s) {
//                subscription = s;
//                subscription.request(Long.MAX_VALUE); // Запрашиваем все документы
//            }
//
//            @Override
//            public void onNext(Document document) {
//                results.add(document);
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                t.printStackTrace();
//                latch.countDown();
//            }
//
//            @Override
//            public void onComplete() {
//                latch.countDown();
//            }
//        });
//        try {
//            if (!latch.await(30, TimeUnit.SECONDS)) {
//                throw new RuntimeException("Timeout waiting for MongoDB operation to complete");
//            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        Date expirationDate = null;

        if (expirationStrategy.getClass() != NoneExpirationAfterWriteStrategy.class) {
            long currentTimeNanos = currentTimeNanos();
            long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos);
            long expiresAt = ttlMillis + TimeUnit.NANOSECONDS.toMillis(currentTimeNanos);
            expirationDate = new Date(expiresAt);
            logger.log(Level.FINE, "Expiration date calculated: {0}", expirationDate);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (originalData == null) {
            // Insert case: document should not exist, use insertOne for proper CAS semantics
            logger.log(Level.FINE, "Insert case - creating new document");
            Document newDocument = new Document("_id", keyBytes)
                    .append("state", newData)
                    .append("expiresAt", expirationDate);

            collection.insertOne(newDocument).subscribe(new Subscriber<com.mongodb.client.result.InsertOneResult>() {
                @Override
                public void onSubscribe(Subscription s) {
                    logger.log(Level.FINE, "Insert onSubscribe - requesting 1 item");
                    s.request(1);
                }

                @Override
                public void onNext(com.mongodb.client.result.InsertOneResult result) {
                    logger.log(Level.FINE, "Insert onNext - insert successful, completing with true");
                    future.complete(true);
                }

                @Override
                public void onError(Throwable t) {
                    // If insert fails due to duplicate key, CAS failed (document already exists)
                    if (isDuplicateKeyError(t)) {
                        logger.log(Level.FINE, "Insert onError - duplicate key detected, CAS failed");
                        future.complete(false);
                    } else {
                        logger.log(Level.WARNING, "Insert onError - unexpected error", t);
                        future.completeExceptionally(t);
                    }
                }

                @Override
                public void onComplete() {
                    if (!future.isDone()) {
                        logger.log(Level.FINE, "Insert onComplete - completing with false (no onNext called)");
                        future.complete(false);
                    }
                }
            });
        } else {
            // Update case: document must exist and state must match originalData
            logger.log(Level.FINE, "Update case - looking for existing document with matching state");
            Document filter = new Document("_id", keyBytes).append("state", originalData);
            Document replacement = new Document("_id", keyBytes)
                    .append("state", newData)
                    .append("expiresAt", expirationDate);

            FindOneAndReplaceOptions options = new FindOneAndReplaceOptions();

            collection.findOneAndReplace(filter, replacement, options).subscribe(new Subscriber<Document>() {
                @Override
                public void onSubscribe(Subscription s) {
                    logger.log(Level.FINE, "Update onSubscribe - requesting 1 item");
                    s.request(1);
                }

                @Override
                public void onNext(Document document) {
                    // findOneAndReplace returns the original document if replacement was successful
                    logger.log(Level.FINE, "Update onNext - document found and replaced, completing with true");
                    future.complete(true);
                }

                @Override
                public void onError(Throwable t) {
                    logger.log(Level.WARNING, "Update onError - error during findOneAndReplace", t);
                    future.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    if (!future.isDone()) {
                        // No document was found/replaced - CAS failed
                        logger.log(Level.FINE, "Update onComplete - no document found/replaced, CAS failed");
                        future.complete(false);
                    }
                }
            });
        }

        logger.log(Level.FINE, "compareAndSwapFuture - returning future");
        return future;
    }

    private boolean isDuplicateKeyError(Throwable t) {
        return t instanceof com.mongodb.MongoWriteException ||
                (t.getMessage() != null && (t.getMessage().contains("duplicate key") ||
                        t.getMessage().contains("E11000")));
    }
}
