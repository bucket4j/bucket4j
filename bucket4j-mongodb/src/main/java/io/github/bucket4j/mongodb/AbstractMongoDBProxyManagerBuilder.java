package io.github.bucket4j.mongodb;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.serialization.Mapper;
import org.bson.Document;

import java.util.Objects;

public abstract class AbstractMongoDBProxyManagerBuilder <K, P extends ProxyManager<K>, B extends AbstractMongoDBProxyManagerBuilder<K, P, B>>
        extends AbstractProxyManagerBuilder<K, P, B> {
    private final MongoCollection<Document> collection;
    private final Mapper<K> keyMapper;
    private String stateFieldName = "state";
    private String expiresAtFieldName = "expiresAt";

    protected AbstractMongoDBProxyManagerBuilder(MongoCollection<Document> collection, Mapper<K> keyMapper) {
        this.collection = collection;
        this.keyMapper = keyMapper;
    }

    public B stateField(String stateColumnName) {
        this.stateFieldName = Objects.requireNonNull(stateColumnName);
        return (B) this;
    }

    public B expiresAtField(String expiresAtColumnName) {
        this.expiresAtFieldName = Objects.requireNonNull(expiresAtColumnName);
        return (B) this;
    }

    public MongoCollection<Document> getCollection() {
        return collection;
    }

    public Mapper<K> getKeyMapper() {
        return keyMapper;
    }

    public String getStateFieldName() {
        return stateFieldName;
    }

    public String getExpiresAtFieldName() {
        return expiresAtFieldName;
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }
}
