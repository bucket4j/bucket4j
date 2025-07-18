package io.github.bucket4j.mongodb_sync;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.expiration.NoneExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import org.bson.Document;
import org.bson.types.Binary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MongoDBSyncCompareAndSwapBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> implements ExpiredEntriesCleaner {
    private final MongoCollection<Document> collection;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final String idFieldName = "_id";
    private final String stateFieldName;
    private final String expiresAtFieldName;

    protected MongoDBSyncCompareAndSwapBasedProxyManager(Bucket4jMongoDBSync.MongoDBSyncCompareAndSwapBasedProxyManagerBuilder<K> builder) {
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
                Document document = collection.find(Filters.eq(idFieldName, keyBytes))
                        .projection(
                                Projections.fields(
                                        Projections.include(stateFieldName),
                                        Projections.excludeId()
                                )
                        )
                        .first();
                if (document != null) {
                    return Optional.ofNullable(document.get(stateFieldName, Binary.class).getData());
                }

                return Optional.empty();
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                Date expirationDate = null;

                if (expirationStrategy.getClass() != NoneExpirationAfterWriteStrategy.class) {
                    long currentTimeNanos = currentTimeNanos();
                    long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos);
                    long expiresAt = ttlMillis + TimeUnit.NANOSECONDS.toMillis(currentTimeNanos);
                    expirationDate = new Date(expiresAt);
                }

                if (originalData == null) {
                    // Insert case: document should not exist, use insertOne for proper CAS semantics
                    try {
                        Document newDocument = new Document(idFieldName, keyBytes)
                                .append(stateFieldName, newData)
                                .append(expiresAtFieldName, expirationDate);

                        collection.insertOne(newDocument);
                        return true;
                    } catch (com.mongodb.MongoWriteException e) {
                        // Document already exists - CAS failed
                        if (isDuplicateKeyError(e)) {
                            return false;
                        }
                        throw e;
                    }
                } else {
                    // Update case: document must exist and state must match originalData
                    Document filter = new Document(idFieldName, keyBytes).append(stateFieldName, originalData);
                    Document replacement = new Document(idFieldName, keyBytes)
                            .append(stateFieldName, newData)
                            .append(expiresAtFieldName, expirationDate);

                    FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.BEFORE);

                    Document documentBeforeReplace = collection.findOneAndReplace(filter, replacement, options);
                    return documentBeforeReplace != null;
                }
            }

            private boolean isDuplicateKeyError(Throwable t) {
                return t instanceof com.mongodb.MongoWriteException ||
                        (t.getMessage() != null && (t.getMessage().contains("duplicate key") ||
                                t.getMessage().contains("E11000")));
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return null;
    }

    @Override
    public void removeProxy(K key) {
        byte[] keyBytes = keyMapper.toBytes(key);
        collection.deleteOne(Filters.eq(idFieldName, keyBytes));
    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    @Override
    public int removeExpired(int batchSize) {
        long currentTimeMillis = System.currentTimeMillis();
        Date currentTime = new Date(currentTimeMillis);

        List<Document> documentsToRemove = collection.find(Filters.lt(expiresAtFieldName, currentTime)).limit(batchSize).into(new ArrayList<>());

        if (documentsToRemove.isEmpty()) {
            return 0;
        }

        List<Object> idsToDelete = documentsToRemove.stream().map(doc -> doc.get(idFieldName)).toList();
        DeleteResult deletionResult = collection.deleteMany(
                Filters.in(idFieldName, idsToDelete)
        );

        return (int) deletionResult.getDeletedCount();
    }
}
