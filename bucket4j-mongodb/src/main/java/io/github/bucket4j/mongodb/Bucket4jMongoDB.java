package io.github.bucket4j.mongodb;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;
import org.bson.Document;

import java.util.Objects;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

public class Bucket4jMongoDB {
    public static MongoDBCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(MongoCollection<Document> collection) {
        return new MongoDBCompareAndSwapBasedProxyManagerBuilder<>(collection, STRING);
    }

    public static <K> MongoDBCompareAndSwapBasedProxyManagerBuilder<K> compareAndSwapBasedBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
        return new MongoDBCompareAndSwapBasedProxyManagerBuilder<>(collection, keyMapper);
    }

    public static class MongoDBCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, MongoDBCompareAndSwapBasedProxyManager<K>, MongoDBCompareAndSwapBasedProxyManagerBuilder<K>> {
        private final MongoCollection<Document> collection;
        private final Mapper<K> keyMapper;

        public MongoDBCompareAndSwapBasedProxyManagerBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
            this.collection = Objects.requireNonNull(collection, "Bucket collection cannot be null");
            this.keyMapper = Objects.requireNonNull(keyMapper);
        }

        @Override
        public MongoDBCompareAndSwapBasedProxyManager<K> build() {
            return new MongoDBCompareAndSwapBasedProxyManager<>(this);
        }

        public MongoCollection<Document> getCollection() {
            return collection;
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
        }
    }
}
