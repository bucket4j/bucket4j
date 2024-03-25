package io.github.bucket4j.mssql;

import java.util.Objects;

import javax.sql.DataSource;

import io.github.bucket4j.distributed.jdbc.AbstractJdbcProxyManagerBuilder;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;

/**
 * Entry point for MSSQL integration
 */
public class Bucket4jMSSQL {

    /**
     * Returns the builder for {@link MSSQLSelectForUpdateBasedProxyManager}
     *
     * @param dataSource
     *
     * @return new instance of {@link MSSQLSelectForUpdateBasedProxyManager}
     */
    public static MSSQLSelectForUpdateBasedProxyManagerBuilder<Long> selectForUpdateBasedBuilder(DataSource dataSource) {
        return new MSSQLSelectForUpdateBasedProxyManagerBuilder<>(dataSource, PrimaryKeyMapper.LONG);
    }

    public static class MSSQLSelectForUpdateBasedProxyManagerBuilder<K> extends AbstractJdbcProxyManagerBuilder<K, MSSQLSelectForUpdateBasedProxyManager<K>, MSSQLSelectForUpdateBasedProxyManagerBuilder<K>> {

        public MSSQLSelectForUpdateBasedProxyManagerBuilder(DataSource dataSource, PrimaryKeyMapper<K> primaryKeyMapper) {
            super(dataSource, primaryKeyMapper);
        }

        @Override
        public MSSQLSelectForUpdateBasedProxyManager<K> build() {
            return new MSSQLSelectForUpdateBasedProxyManager<>(this);
        }

        /**
         * Specifies the type of primary key.
         *
         * @param primaryKeyMapper object responsible for setting primary key value in prepared statement.
         *
         * @return this builder instance
         */
        public <K2> MSSQLSelectForUpdateBasedProxyManagerBuilder<K2> primaryKeyMapper(PrimaryKeyMapper<K2> primaryKeyMapper) {
            super.primaryKeyMapper = (PrimaryKeyMapper) Objects.requireNonNull(primaryKeyMapper);
            return (MSSQLSelectForUpdateBasedProxyManagerBuilder<K2>) this;
        }
    }

}
