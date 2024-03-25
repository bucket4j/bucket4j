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
     * @return new instance of {@link PostgreSQLadvisoryLockBasedProxyManagerBuilder}
     */
    public static PostgreSQLadvisoryLockBasedProxyManagerBuilder<Long> advisoryLockBasedBuilder(DataSource dataSource) {
        return new PostgreSQLadvisoryLockBasedProxyManagerBuilder<>(dataSource, PrimaryKeyMapper.LONG);
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

    public static class PostgreSQLadvisoryLockBasedProxyManagerBuilder<K> extends AbstractJdbcProxyManagerBuilder<K, PostgreSQLadvisoryLockBasedProxyManager<K>, PostgreSQLadvisoryLockBasedProxyManagerBuilder<K>> {

        private LockIdSupplier<K> lockIdSupplier = (LockIdSupplier) LockIdSupplier.DEFAULT;

        public PostgreSQLadvisoryLockBasedProxyManagerBuilder(DataSource dataSource, PrimaryKeyMapper<K> primaryKeyMapper) {
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
        public PostgreSQLadvisoryLockBasedProxyManagerBuilder<K> lockIdSupplier(LockIdSupplier<K> lockIdSupplier) {
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
        public <K2> PostgreSQLadvisoryLockBasedProxyManagerBuilder<K2> primaryKeyMapper(PrimaryKeyMapper<K2> primaryKeyMapper) {
            super.primaryKeyMapper = (PrimaryKeyMapper) Objects.requireNonNull(primaryKeyMapper);
            return (PostgreSQLadvisoryLockBasedProxyManagerBuilder<K2>) this;
        }

        public LockIdSupplier<K> getLockIdSupplier() {
            return lockIdSupplier;
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

    }

}
