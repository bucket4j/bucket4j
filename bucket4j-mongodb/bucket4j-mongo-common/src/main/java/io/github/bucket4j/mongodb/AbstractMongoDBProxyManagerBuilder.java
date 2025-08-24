/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2025 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.mongodb;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.serialization.Mapper;

import java.util.Objects;

/**
 * Base class for MongoDB specific proxy manager builders.
 * <p>
 * This class doesn't have a field containing MongoDB collection since both mongodb-driver-reactivestreams and mongodb-driver-sync
 * classes representing MongoDB collections have the same name and have no common ancestor-classes. As such, it is impossible
 * to create a field that would suit both types of collections.
 * <p>
 *
 * @param <K> type of primary key to be used for bucket identification
 * @param <P> type of ProxyManager to be built
 * @param <B> type of ProxyManagerBuilder
 */
public abstract class AbstractMongoDBProxyManagerBuilder<K, P extends ProxyManager<K>, B extends AbstractMongoDBProxyManagerBuilder<K, P, B>>
        extends AbstractProxyManagerBuilder<K, P, B> {
    private final Mapper<K> keyMapper;
    private String stateFieldName = "state";
    private String expiresAtFieldName = "expiresAt";

    protected AbstractMongoDBProxyManagerBuilder(Mapper<K> keyMapper) {
        this.keyMapper = keyMapper;
    }

    /**
     * Specify the name of the field that will be used to store the bucket state in the MongoDB collection.
     *
     * @param stateColumnName - name of the field containing bucket state
     * @return the builder itself for method chaining.
     */
    public B stateField(String stateColumnName) {
        this.stateFieldName = Objects.requireNonNull(stateColumnName);
        return (B) this;
    }

    /**
     * Specify the name of the field that will be used to store bucket's expiration timestamp in the MongoDB collection.
     *
     * @param expiresAtColumnName - name of the field containing bucket's expiration timestamp
     * @return the builder itself for method chaining.
     */
    public B expiresAtField(String expiresAtColumnName) {
        this.expiresAtFieldName = Objects.requireNonNull(expiresAtColumnName);
        return (B) this;
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
