/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
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

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * @author Maxim Bartkov
 * The class to build {@link SQLProxyConfiguration}
 */
public final class SQLProxyConfigurationBuilder<K> {
    ClientSideConfig clientSideConfig;
    BucketTableSettings tableSettings;
    PrimaryKeyMapper<K> primaryKeyMapper;

    SQLProxyConfigurationBuilder(PrimaryKeyMapper<K> primaryKeyMapper) {
        this.primaryKeyMapper = primaryKeyMapper;
        this.tableSettings = BucketTableSettings.getDefault();
        this.clientSideConfig = ClientSideConfig.getDefault();
    }

    /**
     * @deprecated use {@link SQLProxyConfiguration#builder()}
     *
     * @return the new instance of {@link SQLProxyConfigurationBuilder} configured with {@link PrimaryKeyMapper#LONG} primary key mapper
     */
    public static SQLProxyConfigurationBuilder<Long> builder() {
        return new SQLProxyConfigurationBuilder<>(PrimaryKeyMapper.LONG);
    }

    /**
     * @param clientSideConfig {@link ClientSideConfig} client-side configuration for proxy-manager.
     *                         By default, under the hood uses {@link ClientSideConfig#getDefault}
     * @return {@link SQLProxyConfigurationBuilder}
     */
    public SQLProxyConfigurationBuilder<K> withClientSideConfig(ClientSideConfig clientSideConfig) {
        this.clientSideConfig = Objects.requireNonNull(clientSideConfig);
        return this;
    }

    /**
     * @param tableSettings {@link BucketTableSettings} define a configuration of the table to use as a Buckets store.
     *                      By default, under the hood uses {@link BucketTableSettings#getDefault}
     * @return {@link SQLProxyConfigurationBuilder}
     */
    public SQLProxyConfigurationBuilder<K> withTableSettings(BucketTableSettings tableSettings) {
        this.tableSettings = Objects.requireNonNull(tableSettings);
        return this;
    }

    /**
     * @param primaryKeyMapper Specifies the type of primary key.
     *                         By default, under the hood uses {@link PrimaryKeyMapper#LONG}
     * @return {@link SQLProxyConfigurationBuilder}
     */
    public <T> SQLProxyConfigurationBuilder<T> withPrimaryKeyMapper(PrimaryKeyMapper<T> primaryKeyMapper) {
        this.primaryKeyMapper = (PrimaryKeyMapper<K>) Objects.requireNonNull(primaryKeyMapper);
        return (SQLProxyConfigurationBuilder<T>) this;
    }

    /**
     * The method takes a {@link DataSource} as a required parameter
     *
     * @param dataSource - a database configuration
     * @return {@link SQLProxyConfiguration}
     */
    public SQLProxyConfiguration<K> build(DataSource dataSource) {
        if (dataSource == null) {
            throw new BucketExceptions.BucketExecutionException("DataSource cannot be null");
        }
        return new SQLProxyConfiguration<>(this, dataSource);
    }
}
