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
package io.github.bucket4j.postgresql;

import java.util.Objects;

import javax.sql.DataSource;

import io.github.bucket4j.distributed.jdbc.AbstractJdbcProxyManagerBuilder;
import io.github.bucket4j.distributed.jdbc.LockIdSupplier;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;

/**
 * Entry point for PostgreSQL integration
 */
public class Bucket4jPostgreSQL {

    /**
     * Returns the builder for {@link PostgreSQLadvisoryLockBasedProxyManager}
     *
     * @param dataSource
     *
     * @return new instance of {@link PostgreSQLAdvisoryLockBasedProxyManagerBuilder}
     */
    public static PostgreSQLAdvisoryLockBasedProxyManagerBuilder<Long> advisoryLockBasedBuilder(DataSource dataSource) {
        return new PostgreSQLAdvisoryLockBasedProxyManagerBuilder<>(dataSource, PrimaryKeyMapper.LONG);
    }

    /**
     * Returns the builder for {@link PostgreSQLSelectForUpdateBasedProxyManager}
     *
     * @param dataSource
     *
     * @return new instance of {@link PostgreSQLSelectForUpdateBasedProxyManagerBuilder}
     */
    public static PostgreSQLSelectForUpdateBasedProxyManagerBuilder<Long> selectForUpdateBasedBuilder(DataSource dataSource) {
        return new PostgreSQLSelectForUpdateBasedProxyManagerBuilder<>(dataSource, PrimaryKeyMapper.LONG);
    }

    public static class PostgreSQLAdvisoryLockBasedProxyManagerBuilder<K> extends AbstractJdbcProxyManagerBuilder<K, PostgreSQLadvisoryLockBasedProxyManager<K>, PostgreSQLAdvisoryLockBasedProxyManagerBuilder<K>> {

        private LockIdSupplier<K> lockIdSupplier = (LockIdSupplier) LockIdSupplier.DEFAULT;
        private String lockColumn = "explicit_lock";

        public PostgreSQLAdvisoryLockBasedProxyManagerBuilder(DataSource dataSource, PrimaryKeyMapper<K> primaryKeyMapper) {
            super(dataSource, primaryKeyMapper);
        }

        @Override
        public PostgreSQLadvisoryLockBasedProxyManager<K> build() {
            return new PostgreSQLadvisoryLockBasedProxyManager<>(this);
        }

        /**
         * Specifies object that responsible to calculate lock from primary key
         *
         * @param lockIdSupplier object that responsible to calculate lock from primary key
         *
         * @return this builder instance
         */
        public PostgreSQLAdvisoryLockBasedProxyManagerBuilder<K> lockIdSupplier(LockIdSupplier<K> lockIdSupplier) {
            this.lockIdSupplier = Objects.requireNonNull(lockIdSupplier);
            return this;
        }

        /**
         * Specifies the type of primary key.
         *
         * @param primaryKeyMapper object responsible for setting primary key value in prepared statement.
         *
         * @return this builder instance
         */
        public <K2> PostgreSQLAdvisoryLockBasedProxyManagerBuilder<K2> primaryKeyMapper(PrimaryKeyMapper<K2> primaryKeyMapper) {
            super.primaryKeyMapper = (PrimaryKeyMapper) Objects.requireNonNull(primaryKeyMapper);
            return (PostgreSQLAdvisoryLockBasedProxyManagerBuilder<K2>) this;
        }

        /**
         * Specifies the name of column to store advisory_lock instead of default "explicit_lock".
         *
         * @param lockColumn the name of column to store advisory_lock
         *
         * @return this builder instance
         */
        public PostgreSQLAdvisoryLockBasedProxyManagerBuilder<K> lockColumn(String lockColumn) {
            this.lockColumn = Objects.requireNonNull(lockColumn);
            return this;
        }

        public LockIdSupplier<K> getLockIdSupplier() {
            return lockIdSupplier;
        }

        public String getLockColumn() {
            return lockColumn;
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }
    }

    public static class PostgreSQLSelectForUpdateBasedProxyManagerBuilder<K> extends AbstractJdbcProxyManagerBuilder<K, PostgreSQLSelectForUpdateBasedProxyManager<K>, PostgreSQLSelectForUpdateBasedProxyManagerBuilder<K>> {

        public PostgreSQLSelectForUpdateBasedProxyManagerBuilder(DataSource dataSource, PrimaryKeyMapper<K> primaryKeyMapper) {
            super(dataSource, primaryKeyMapper);
        }

        @Override
        public PostgreSQLSelectForUpdateBasedProxyManager<K> build() {
            return new PostgreSQLSelectForUpdateBasedProxyManager<>(this);
        }

        /**
         * Specifies the type of primary key.
         *
         * @param primaryKeyMapper object responsible for setting primary key value in prepared statement.
         *
         * @return this builder instance
         */
        public <K2> PostgreSQLSelectForUpdateBasedProxyManagerBuilder<K2> primaryKeyMapper(PrimaryKeyMapper<K2> primaryKeyMapper) {
            super.primaryKeyMapper = (PrimaryKeyMapper) Objects.requireNonNull(primaryKeyMapper);
            return (PostgreSQLSelectForUpdateBasedProxyManagerBuilder<K2>) this;
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

    }

}
