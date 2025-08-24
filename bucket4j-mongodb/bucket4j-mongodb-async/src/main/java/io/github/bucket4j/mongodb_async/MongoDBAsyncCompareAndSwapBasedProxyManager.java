package io.github.bucket4j.mongodb_async;

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
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The extension of Bucket4j library addressed to support MongoDB with a Reactive-Streams-based version of MongoDB Java driver.
 * <p>
 * Compare-And-Swap-based proxy manager for MongoDB that uses ReactiveStreams to provide asynchronous operations.
 * <p>
 * This implementation provides CAS functionality and solves concurrency related problems by utilizing MongoDB's atomic operations
 * {@link MongoCollection#insertOne(Object)} and {@link MongoCollection#findOneAndReplace(Bson, Object)}.
 *
 * @param <K> type of primary key
 */
public class MongoDBAsyncCompareAndSwapBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> implements ExpiredEntriesCleaner {
    private final MongoCollection<Document> collection;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final String idFieldName = "_id";
    private final String stateFieldName;
    private final String expiresAtFieldName;

    protected MongoDBAsyncCompareAndSwapBasedProxyManager(Bucket4jMongoDBAsync.MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<K> builder) {
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
                return getBucketStateFuture(keyBytes).join();
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
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
                return getBucketStateFuture(keyBytes);
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                return compareAndSwapFuture(keyBytes, originalData, newData, newState);
            }
        };
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        byte[] keyBytes = keyMapper.toBytes(key);
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
        removeAsync(key).join();
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
        CompletableFuture<Document> future = new CompletableFuture<>();

        collection.find(Filters.eq(idFieldName, key))
                .projection(
                        Projections.fields(
                                Projections.include(stateFieldName),
                                Projections.excludeId()
                        )
                )
                .first()
                .subscribe(new Subscriber<>() {
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

        return future.thenApply(bucketState -> {
            if (bucketState == null) {
                return Optional.empty();
            } else {
                return Optional.ofNullable(bucketState.get(stateFieldName, Binary.class)).map(Binary::getData);
            }
        });
    }

    private CompletableFuture<Boolean> compareAndSwapFuture(byte[] keyBytes, byte[] originalData, byte[] newData, RemoteBucketState newState) {
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
            Document newDocument = new Document(idFieldName, keyBytes)
                    .append(stateFieldName, newData)
                    .append(expiresAtFieldName, expirationDate);

            collection.insertOne(newDocument).subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(1);
                }

                @Override
                public void onNext(com.mongodb.client.result.InsertOneResult result) {
                    future.complete(true);
                }

                @Override
                public void onError(Throwable t) {
                    // If insert fails due to duplicate key, CAS failed (document already exists)
                    if (isDuplicateKeyError(t)) {
                        future.complete(false);
                    } else {
                        future.completeExceptionally(t);
                    }
                    onComplete();

                }

                @Override
                public void onComplete() {
                    if (!future.isDone()) {
                        future.complete(false);
                    }
                }
            });
        } else {
            // Update case: document must exist and state must match originalData
            Document filter = new Document(idFieldName, keyBytes).append(stateFieldName, originalData);
            Document replacement = new Document(idFieldName, keyBytes)
                    .append(stateFieldName, newData)
                    .append(expiresAtFieldName, expirationDate);

            FindOneAndReplaceOptions options = new FindOneAndReplaceOptions();

            collection.findOneAndReplace(filter, replacement, options).subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(1);
                }

                @Override
                public void onNext(Document document) {
                    // findOneAndReplace returns the original document if replacement was successful
                    future.complete(true);
                }

                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                    onComplete();
                }

                @Override
                public void onComplete() {
                    if (!future.isDone()) {
                        // No document was found/replaced - CAS failed
                        future.complete(false);
                    }
                }
            });
        }

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
                new Subscriber<>() {
                    final List<Document> documents = new ArrayList<>();

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
        ).subscribe(new Subscriber<>() {
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
