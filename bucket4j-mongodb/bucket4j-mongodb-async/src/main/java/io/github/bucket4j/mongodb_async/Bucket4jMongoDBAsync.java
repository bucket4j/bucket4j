package io.github.bucket4j.mongodb_async;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.mongodb.AbstractMongoDBProxyManagerBuilder;
import org.bson.Document;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

public class Bucket4jMongoDBAsync {
    public static MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(MongoCollection<Document> collection) {
        return new MongoDBAsyncCompareAndSwapBasedProxyManagerBuilder<>(collection, STRING);
    }

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
