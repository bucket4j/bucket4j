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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MongoDBCompareAndSwapBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {
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
        ByteBuffer keyBytes = ByteBuffer.wrap(keyMapper.toBytes(key));
        CompletableFuture<Void> future = new CompletableFuture<>();

        collection
                .deleteOne(Filters.eq("_id", keyBytes))
                .subscribe(new MongoDBUtilitySubscriber<DeleteResult, Void>(future));

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

    private CompletableFuture<Optional<byte[]>> getBucketStateFuture(byte[] key) {
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
                return Optional.empty();
            } else {
                return Optional.ofNullable(bucketState.get("state", Binary.class)).map(Binary::getData);
            }
        });
    }

    private CompletableFuture<Boolean> compareAndSwapFuture(byte[] keyBytes, byte[] originalData, byte[] newData, RemoteBucketState newState) {
        List<Document> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        collection.find().subscribe(new Subscriber<Document>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                subscription.request(Long.MAX_VALUE); // Запрашиваем все документы
            }

            @Override
            public void onNext(Document document) {
                results.add(document);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout waiting for MongoDB operation to complete");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Document filter = new Document("_id", keyBytes);

//        if (originalData != null) {
            filter.append("state", originalData);
//        } else {
//            filter.append("state", new Document("$exists", false));
//        }

        Date expirationDate = null;

        if (expirationStrategy.getClass() != NoneExpirationAfterWriteStrategy.class) {
            long currentTimeNanos = currentTimeNanos();
            long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos);
            long expiresAt = ttlMillis + TimeUnit.NANOSECONDS.toMillis(currentTimeNanos);
            expirationDate = new Date(expiresAt);
        }

        Document replacement = new Document("_id", keyBytes).append("state", newData).append("expiresAt", expirationDate);
        FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().upsert(true);

        CompletableFuture<Boolean> future =  new CompletableFuture<Boolean>();

        collection.findOneAndReplace(filter, replacement, options).subscribe(new Subscriber<Document>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(Document document) {
                future.complete(true);
            }

            @Override
            public void onError(Throwable t) {
               future.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                if (!future.isDone()) {
                    future.complete(false);
                }
            }
        });

        return future;
    }
}
