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

import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import javax.sql.DataSource;

@Deprecated
public class SQLProxyConfiguration<K> {

    private final DataSource dataSource;
    private final ClientSideConfig clientSideConfig;
    private final BucketTableSettings tableSettings;
    private final PrimaryKeyMapper<K> primaryKeyMapper;

    /**
     * @return the new instance of {@link SQLProxyConfigurationBuilder} configured with {@link PrimaryKeyMapper#LONG} primary key mapper
     */
    public static SQLProxyConfigurationBuilder<Long> builder() {
        return new SQLProxyConfigurationBuilder<>(PrimaryKeyMapper.LONG);
    }

    SQLProxyConfiguration(SQLProxyConfigurationBuilder<K> builder, DataSource dataSource) {
        this.dataSource = dataSource;
        this.clientSideConfig = builder.clientSideConfig;
        this.tableSettings = builder.tableSettings;
        this.primaryKeyMapper = builder.primaryKeyMapper;
    }

    public String getIdName() {
        return tableSettings.getIdName();
    }

    public String getStateName() {
        return tableSettings.getStateName();
    }

    public String getTableName() {
        return tableSettings.getTableName();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public ClientSideConfig getClientSideConfig() {
        return clientSideConfig;
    }

    public PrimaryKeyMapper<K> getPrimaryKeyMapper() {
        return primaryKeyMapper;
    }

}
