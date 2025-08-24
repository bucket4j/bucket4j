package io.github.bucket4j.mongodb_sync;

import com.mongodb.client.MongoCollection;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.mongodb.AbstractMongoDBProxyManagerBuilder;
import org.bson.Document;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

/**
 * Entry point for MongoDB integration that uses Synchronous MongoDB Java Driver from org.mongodb.mongodb-driver-sync.
 */
public class Bucket4jMongoDBSync {
    /**
     * Return the builder for {@link MongoDBSyncCompareAndSwapBasedProxyManager} that uses {@link String} values as keys by default.
     *
     * @param collection MongoDB collection that holds buckets.
     * @return New instance of {@link MongoDBSyncCompareAndSwapBasedProxyManagerBuilder}
     */
    public static MongoDBSyncCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(MongoCollection<Document> collection) {
        return new MongoDBSyncCompareAndSwapBasedProxyManagerBuilder<>(collection, STRING);
    }

    /**
     * Return the builder for {@link MongoDBSyncCompareAndSwapBasedProxyManager} that uses values of type {@link K} as keys.
     *
     * @param collection MongoDB collection that holds buckets.
     * @param keyMapper  An implementation of interface {@link Mapper} that will be used for mapping keys from {@link K} to byte[].
     * @param <K>        The type that will be used for primary key mapping.
     * @return New instance of {@link MongoDBSyncCompareAndSwapBasedProxyManagerBuilder}
     */
    public static <K> MongoDBSyncCompareAndSwapBasedProxyManagerBuilder<K> compareAndSwapBasedBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
        return new MongoDBSyncCompareAndSwapBasedProxyManagerBuilder<>(collection, keyMapper);
    }

    public static class MongoDBSyncCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractMongoDBProxyManagerBuilder<K, MongoDBSyncCompareAndSwapBasedProxyManager<K>, MongoDBSyncCompareAndSwapBasedProxyManagerBuilder<K>> {
        private final MongoCollection<Document> collection;

        public MongoDBSyncCompareAndSwapBasedProxyManagerBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
            super(keyMapper);
            this.collection = collection;
        }

        public MongoCollection<Document> getCollection() {
            return collection;
        }

        @Override
        public MongoDBSyncCompareAndSwapBasedProxyManager<K> build() {
            return new MongoDBSyncCompareAndSwapBasedProxyManager<>(this);
        }
    }
}

