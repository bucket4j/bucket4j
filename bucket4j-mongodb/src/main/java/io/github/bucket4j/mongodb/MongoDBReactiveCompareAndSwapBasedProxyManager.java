package io.github.bucket4j.mongodb;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.expiration.NoneExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoDBReactiveCompareAndSwapBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> implements ExpiredEntriesCleaner {
    private static final Logger logger = Logger.getLogger(MongoDBReactiveCompareAndSwapBasedProxyManager.class.getName());

    private final MongoCollection<Document> collection;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final String idFieldName = "_id";
    private final String stateFieldName;
    private final String expiresAtFieldName;

    protected MongoDBReactiveCompareAndSwapBasedProxyManager(MongoDBCompareAndSwapBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.collection = builder.getCollection();
        this.keyMapper = builder.getKeyMapper();
        Optional<ExpirationAfterWriteStrategy> expirationStrategyFromClientConfig = builder.getClientSideConfig().getExpirationAfterWriteStrategy();
        this.expirationStrategy = expirationStrategyFromClientConfig.orElseGet(ExpirationAfterWriteStrategy::none);
        this.stateFieldName = builder.getStateFieldName();
        this.expiresAtFieldName = builder.getExpiresAtFieldName();
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
        byte[] keyBytes = keyMapper.toBytes(key);
        logger.log(Level.FINE, "removeAsync - removing document with key");
        CompletableFuture<Void> future = new CompletableFuture<>();

        collection
                .deleteOne(Filters.eq(idFieldName, keyBytes))
                .subscribe(new Subscriber<>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(DeleteResult deleteResult) {
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

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    private CompletableFuture<Optional<byte[]>> getBucketStateFuture(byte[] key) {
        logger.log(Level.FINE, "getBucketStateFuture - looking for document with key length: {0}", key.length);
        CompletableFuture<Document> future = new CompletableFuture<>();

        collection.find(Filters.eq(idFieldName, key))
                .projection(
                        Projections.fields(
                                Projections.include(stateFieldName),
                                Projections.excludeId()
                        )
                )
                .first()
                .subscribe(new Subscriber<Document>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        System.out.println("[MongoDB-CAS] getBucketStateFuture onSubscribe - requesting 1 item");
                        s.request(1);
                    }

                    @Override
                    public void onNext(Document document) {
                        System.out.println("[MongoDB-CAS] getBucketStateFuture onNext - document found");
                        future.complete(document);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("[MongoDB-CAS] getBucketStateFuture onError - error: " + t.getMessage());
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        if (!future.isDone()) {
                            System.out.println("[MongoDB-CAS] getBucketStateFuture onComplete - no document found");
                            future.complete(null);
                        }
                    }
                });

        return future.thenApply(bucketState -> {
            if (bucketState == null) {
                logger.log(Level.FINE, "getBucketStateFuture - document not found, returning empty");
                return Optional.empty();
            } else {
                logger.log(Level.FINE, "getBucketStateFuture - document found, extracting state");
                return Optional.ofNullable(bucketState.get(stateFieldName, Binary.class)).map(Binary::getData);
            }
        });
    }

    private CompletableFuture<Boolean> compareAndSwapFuture(byte[] keyBytes, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        logger.log(Level.FINE, "Starting compareAndSwapFuture - originalData: {0}, newData length: {1}",
                new Object[]{originalData == null ? "null" : "[" + originalData.length + " bytes]", newData.length});

        Date expirationDate = null;

        if (expirationStrategy.getClass() != NoneExpirationAfterWriteStrategy.class) {
            long currentTimeNanos = currentTimeNanos();
            long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos);
            long expiresAt = ttlMillis + TimeUnit.NANOSECONDS.toMillis(currentTimeNanos);
            expirationDate = new Date(expiresAt);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        if (originalData == null) {
            // Insert case: document should not exist, use insertOne for proper CAS semantics
            logger.log(Level.FINE, "Insert case - creating new document");
            Document newDocument = new Document(idFieldName, keyBytes)
                    .append(stateFieldName, newData)
                    .append(expiresAtFieldName, expirationDate);

            collection.insertOne(newDocument).subscribe(new Subscriber<>() {
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
                    ;
                    onComplete();

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
            Document filter = new Document(idFieldName, keyBytes).append(stateFieldName, originalData);
            Document replacement = new Document(idFieldName, keyBytes)
                    .append(stateFieldName, newData)
                    .append(expiresAtFieldName, expirationDate);

            FindOneAndReplaceOptions options = new FindOneAndReplaceOptions();

            collection.findOneAndReplace(filter, replacement, options).subscribe(new Subscriber<>() {
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
                    onComplete();
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

    @Override
    public int removeExpired(int batchSize) {
        long currentTimeMillis = System.currentTimeMillis();
        Date currentTime = new Date(currentTimeMillis);

        CompletableFuture<List<Document>> documentRetrivalFuture = new CompletableFuture<>();

        collection.find(Filters.lt(expiresAtFieldName, currentTime)).limit(batchSize).subscribe(
                new Subscriber<Document>() {
                    List<Document> documents = new ArrayList<>();

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                    }

                    @Override
                    public void onNext(Document document) {
                        documents.add(document);
                    }

                    @Override
                    public void onError(Throwable t) {
                        documentRetrivalFuture.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        documentRetrivalFuture.complete(documents);
                    }
                }
        );

        List<Document> documentsToRemove = documentRetrivalFuture.join();

        CompletableFuture<Integer> deletionFuture = new CompletableFuture<>();

        if (documentsToRemove.isEmpty()) {
            return 0;
        }

        List<Object> idsToDelete = documentsToRemove.stream().map(doc -> doc.get(idFieldName)).toList();

        collection.deleteMany(
                Filters.in(idFieldName, idsToDelete)
        ).subscribe(new Subscriber<DeleteResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(DeleteResult deleteResult) {
                deletionFuture.complete((int) deleteResult.getDeletedCount());
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.WARNING, "Error during expired entries cleanup", t);
                deletionFuture.complete(0);
            }

            @Override
            public void onComplete() {
                if (!deletionFuture.isDone()) {
                    deletionFuture.complete(0);
                }
            }
        });

        return deletionFuture.join();
    }
}
