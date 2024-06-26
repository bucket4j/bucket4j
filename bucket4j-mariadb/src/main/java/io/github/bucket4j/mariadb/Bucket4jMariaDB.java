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
package io.github.bucket4j.mariadb;

import java.util.Objects;

import javax.sql.DataSource;

import io.github.bucket4j.distributed.jdbc.AbstractJdbcProxyManagerBuilder;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;

/**
 * Entry point for MariaDB integration
 */
public class Bucket4jMariaDB {

    /**
     * Returns the builder for {@link MariaDBSelectForUpdateBasedProxyManager}
     *
     * @param dataSource
     *
     * @return new instance of {@link MariaDBSelectForUpdateBasedProxyManager}
     */
    public static MariaDBSelectForUpdateBasedProxyManagerBuilder<Long> selectForUpdateBasedBuilder(DataSource dataSource) {
        return new MariaDBSelectForUpdateBasedProxyManagerBuilder<>(dataSource, PrimaryKeyMapper.LONG);
    }

    public static class MariaDBSelectForUpdateBasedProxyManagerBuilder<K> extends AbstractJdbcProxyManagerBuilder<K, MariaDBSelectForUpdateBasedProxyManager<K>, MariaDBSelectForUpdateBasedProxyManagerBuilder<K>> {

        public MariaDBSelectForUpdateBasedProxyManagerBuilder(DataSource dataSource, PrimaryKeyMapper<K> primaryKeyMapper) {
            super(dataSource, primaryKeyMapper);
        }

        @Override
        public MariaDBSelectForUpdateBasedProxyManager<K> build() {
            return new MariaDBSelectForUpdateBasedProxyManager<>(this);
        }

        /**
         * Specifies the type of primary key.
         *
         * @param primaryKeyMapper object responsible for setting primary key value in prepared statement.
         *
         * @return this builder instance
         */
        public <K2> MariaDBSelectForUpdateBasedProxyManagerBuilder<K2> primaryKeyMapper(PrimaryKeyMapper<K2> primaryKeyMapper) {
            super.primaryKeyMapper = (PrimaryKeyMapper) Objects.requireNonNull(primaryKeyMapper);
            return (MariaDBSelectForUpdateBasedProxyManagerBuilder<K2>) this;
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }
    }

}
