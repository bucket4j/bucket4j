package io.github.bucket4j.mariadb;

import java.util.Objects;

import javax.sql.DataSource;

import io.github.bucket4j.distributed.jdbc.AbstractJdbcProxyManagerBuilder;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfigurationBuilder;

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
         * @return {@link SQLProxyConfigurationBuilder}
         */
        public <K2> MariaDBSelectForUpdateBasedProxyManagerBuilder<K2> primaryKeyMapper(PrimaryKeyMapper<K2> primaryKeyMapper) {
            super.primaryKeyMapper = (PrimaryKeyMapper) Objects.requireNonNull(primaryKeyMapper);
            return (MariaDBSelectForUpdateBasedProxyManagerBuilder<K2>) this;
        }
    }

}
