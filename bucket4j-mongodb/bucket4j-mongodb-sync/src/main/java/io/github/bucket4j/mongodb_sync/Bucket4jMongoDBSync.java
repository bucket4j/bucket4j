package io.github.bucket4j.mongodb_sync;

import com.mongodb.client.MongoCollection;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.mongodb.AbstractMongoDBProxyManagerBuilder;
import org.bson.Document;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

public class Bucket4jMongoDBSync {
    public static MongoDBSyncCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(MongoCollection<Document> collection) {
        return new MongoDBSyncCompareAndSwapBasedProxyManagerBuilder<>(collection, STRING);
    }

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

