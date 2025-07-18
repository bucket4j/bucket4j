package io.github.bucket4j.mongodb;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.github.bucket4j.distributed.serialization.Mapper;
import org.bson.Document;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

public class Bucket4jMongoDB {
    public static MongoDBCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(MongoCollection<Document> collection) {
        return new MongoDBCompareAndSwapBasedProxyManagerBuilder<>(collection, STRING);
    }

    public static <K> MongoDBCompareAndSwapBasedProxyManagerBuilder<K> compareAndSwapBasedBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
        return new MongoDBCompareAndSwapBasedProxyManagerBuilder<>(collection, keyMapper);
    }

    public static class MongoDBCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractMongoDBProxyManagerBuilder<K, MongoDBReactiveCompareAndSwapBasedProxyManager<K>, MongoDBCompareAndSwapBasedProxyManagerBuilder<K>> {
        public MongoDBCompareAndSwapBasedProxyManagerBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
            super(collection, keyMapper);
        }

        @Override
        public MongoDBReactiveCompareAndSwapBasedProxyManager<K> build() {
            return new MongoDBReactiveCompareAndSwapBasedProxyManager<>(this);
        }
    }
}
