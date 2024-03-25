package io.github.bucket4j.oracle;

import java.util.Objects;

import javax.sql.DataSource;

import io.github.bucket4j.distributed.jdbc.AbstractJdbcProxyManagerBuilder;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;

/**
 * Entry point for Oracle Database integration
 */
public class Bucket4jOracle {

    /**
     * Returns the builder for {@link OracleSelectForUpdateBasedProxyManager}
     *
     * @param dataSource
     *
     * @return new instance of {@link OracleSelectForUpdateBasedProxyManager}
     */
    public static OracleSelectForUpdateBasedProxyManagerBuilder<Long> selectForUpdateBasedBuilder(DataSource dataSource) {
        return new OracleSelectForUpdateBasedProxyManagerBuilder<>(dataSource, PrimaryKeyMapper.LONG);
    }

    public static class OracleSelectForUpdateBasedProxyManagerBuilder<K> extends AbstractJdbcProxyManagerBuilder<K, OracleSelectForUpdateBasedProxyManager<K>, OracleSelectForUpdateBasedProxyManagerBuilder<K>> {

        public OracleSelectForUpdateBasedProxyManagerBuilder(DataSource dataSource, PrimaryKeyMapper<K> primaryKeyMapper) {
            super(dataSource, primaryKeyMapper);
        }

        @Override
        public OracleSelectForUpdateBasedProxyManager<K> build() {
            return new OracleSelectForUpdateBasedProxyManager<>(this);
        }

        /**
         * Specifies the type of primary key.
         *
         * @param primaryKeyMapper object responsible for setting primary key value in prepared statement.
         *
         * @return this builder instance
         */
        public <K2> OracleSelectForUpdateBasedProxyManagerBuilder<K2> primaryKeyMapper(PrimaryKeyMapper<K2> primaryKeyMapper) {
            super.primaryKeyMapper = (PrimaryKeyMapper) Objects.requireNonNull(primaryKeyMapper);
            return (OracleSelectForUpdateBasedProxyManagerBuilder<K2>) this;
        }
    }

}
