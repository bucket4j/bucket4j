package io.github.bucket4j.mysql;

import java.util.Objects;

import javax.sql.DataSource;

import io.github.bucket4j.distributed.jdbc.AbstractJdbcProxyManagerBuilder;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;

/**
 * Entry point for MySQL integration
 */
public class Bucket4jMySQL {

    /**
     * Returns the builder for {@link MySQLSelectForUpdateBasedProxyManager}
     *
     * @param dataSource
     *
     * @return new instance of {@link MySQLSelectForUpdateBasedProxyManager}
     */
    public static MySQLSelectForUpdateBasedProxyManagerBuilder<Long> selectForUpdateBasedBuilder(DataSource dataSource) {
        return new MySQLSelectForUpdateBasedProxyManagerBuilder<>(dataSource, PrimaryKeyMapper.LONG);
    }

    public static class MySQLSelectForUpdateBasedProxyManagerBuilder<K> extends AbstractJdbcProxyManagerBuilder<K, MySQLSelectForUpdateBasedProxyManager<K>, MySQLSelectForUpdateBasedProxyManagerBuilder<K>> {

        public MySQLSelectForUpdateBasedProxyManagerBuilder(DataSource dataSource, PrimaryKeyMapper<K> primaryKeyMapper) {
            super(dataSource, primaryKeyMapper);
        }

        @Override
        public MySQLSelectForUpdateBasedProxyManager<K> build() {
            return new MySQLSelectForUpdateBasedProxyManager<>(this);
        }

        /**
         * Specifies the type of primary key.
         *
         * @param primaryKeyMapper object responsible for setting primary key value in prepared statement.
         *
         * @return this builder instance
         */
        public <K2> MySQLSelectForUpdateBasedProxyManagerBuilder<K2> primaryKeyMapper(PrimaryKeyMapper<K2> primaryKeyMapper) {
            super.primaryKeyMapper = (PrimaryKeyMapper) Objects.requireNonNull(primaryKeyMapper);
            return (MySQLSelectForUpdateBasedProxyManagerBuilder<K2>) this;
        }
    }

}
