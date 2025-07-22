package io.github.bucket4j.mongodb_async;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.mongodb.AbstractMongoDBProxyManagerBuilder;
import org.bson.Document;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

/**
 * Entry point for MongoDB integration that uses Reactive Streams-based MongoDB Java Driver from org.mongodb.mongodb-driver-reactivestreams.
 */
public class Bucket4jMongoDBAsync {
    /**
     * Return the builder for {@link MongoDBAsyncCompareAndSwapBasedProxyManager} that uses {@link String} values as keys by default.
     *
     * @param collection MongoDB collection that holds buckets.
     * @return New instance of {@link MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder}
     */
    public static MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(MongoCollection<Document> collection) {
        return new MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<>(collection, STRING);
    }

    /**
     * Return the builder for {@link MongoDBAsyncCompareAndSwapBasedProxyManager} that uses values of type {@link K} as keys.
     *
     * @param collection MongoDB collection that holds buckets.
     * @param keyMapper  An implementation of interface {@link Mapper} that will be used for mapping keys from {@link K} to byte[].
     * @param <K>        The type that will be used for key mapping.
     * @return New instance of {@link MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder}
     */
    public static <K> MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<K> compareAndSwapBasedBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
        return new MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<>(collection, keyMapper);
    }

    public static class MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractMongoDBProxyManagerBuilder<K, MongoDBAsyncCompareAndSwapBasedProxyManager<K>, MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<K>> {
        private final MongoCollection<Document> collection;

        public MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
            super(keyMapper);
            this.collection = collection;
        }

        public MongoCollection<Document> getCollection() {
            return collection;
        }

        @Override
        public MongoDBAsyncCompareAndSwapBasedProxyManager<K> build() {
            return new MongoDBAsyncCompareAndSwapBasedProxyManager<>(this);
        }
    }
}
