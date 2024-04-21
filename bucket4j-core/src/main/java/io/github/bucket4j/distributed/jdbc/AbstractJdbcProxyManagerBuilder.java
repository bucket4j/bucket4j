/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
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
package io.github.bucket4j.distributed.jdbc;

import java.util.Objects;

import javax.sql.DataSource;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.ProxyManager;

/**
 * Base class for all JDBC proxy-manager builders.
 *
 * @param <K> type of key
 * @param <P> type of proxy manager that is being build
 * @param <B> the type of builder extending {@link AbstractJdbcProxyManagerBuilder}
 */
public abstract class AbstractJdbcProxyManagerBuilder<K, P extends ProxyManager<K>, B extends AbstractJdbcProxyManagerBuilder<K, P, B>>
        extends AbstractProxyManagerBuilder<K, P, B> {

    private final DataSource dataSource;
    protected PrimaryKeyMapper<K> primaryKeyMapper;

    private String tableName = "bucket";;
    private String idColumnName = "state";
    private String stateColumnName = "id";
    private String expiresAtColumnName = "expires_at";

    public AbstractJdbcProxyManagerBuilder(DataSource dataSource, PrimaryKeyMapper<K> primaryKeyMapper) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.primaryKeyMapper = Objects.requireNonNull(primaryKeyMapper);
    }

    /**
     * Specifies name of table to use as a Buckets store
     *
     * @param tableName of table to use as a Buckets store
     *
     * @return this builder instance
     */
    public B table(String tableName) {
        this.tableName = Objects.requireNonNull(tableName);
        return (B) this;
    }

    /**
     * Specifies name of primary key in buckets table
     *
     * @param idColumnName name of primary key in buckets table
     *
     * @return this builder instance
     */
    public B idColumn(String idColumnName) {
        this.idColumnName = Objects.requireNonNull(idColumnName);
        return (B) this;
    }

    /**
     * Specifies name column that used to store a state of bucket
     *
     * @param stateColumnName name column that used to store a state of bucket
     *
     * @return this builder instance
     */
    public B stateColumn(String stateColumnName) {
        this.stateColumnName = Objects.requireNonNull(stateColumnName);
        return (B) this;
    }

    public B expiresAtColumn(String expiresAtColumnName) {
        this.expiresAtColumnName = Objects.requireNonNull(expiresAtColumnName);
        return (B) this;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public PrimaryKeyMapper<K> getPrimaryKeyMapper() {
        return primaryKeyMapper;
    }

    public String getTableName() {
        return tableName;
    }

    public String getIdColumnName() {
        return idColumnName;
    }

    public String getStateColumnName() {
        return stateColumnName;
    }

    public String getExpiresAtColumnName() {
        return expiresAtColumnName;
    }
}
