package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockBasedTransaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Objects;

/**
 * @author Maxim Bartkov
 * The extension of Bucket4j library addressed to support <a href="https://www.postgresql.org/">PostgreSQL</a>
 * To start work with the PostgreSQL extension you must create a table, which will include the possibility to work with buckets
 * In order to do this, your table should include the next columns: id as a PRIMARY KEY (BIGINT) and state (BYTEA)
 * To define column names, {@link PostgreSQLProxyConfiguration} include {@link BucketTableSettings} which takes settings for the table to work with Bucket4j
 * @see {@link PostgreSQLProxyConfigurationBuilder} to get more information how to build {@link PostgreSQLProxyConfiguration}
 */
public class PostgreSQLProxyManager extends AbstractLockBasedProxyManager<Long> {

    private final DataSource dataSource;
    private final PostgreSQLProxyConfiguration configuration;
    private final String removeSqlQuery;

    /**
     *
     * @param configuration {@link PostgreSQLProxyConfiguration} configuration.
     */
    public PostgreSQLProxyManager(PostgreSQLProxyConfiguration configuration) {
        super(configuration.getClientSideConfig());
        this.dataSource = Objects.requireNonNull(configuration.getDataSource());
        this.configuration = configuration;
        this.removeSqlQuery = MessageFormat.format("DELETE FROM {0} WHERE {1} = ?", configuration.getTableName(), configuration.getIdName());
    }

    @Override
    protected LockBasedTransaction allocateTransaction(Long key) {
        try {
            return new PostgreSQLLockBasedTransactionFactory(key, configuration, dataSource.getConnection()).getLockBasedTransaction(configuration.getLockBasedTransactionType());
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

    @Override
    protected void releaseTransaction(LockBasedTransaction transaction) {
        try {
            ((AbstractPostgreSQLLockBasedTransaction) transaction).getConnection().close();
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

    @Override
    public void removeProxy(Long key) {
        try {
            Connection connection = dataSource.getConnection();
            try(PreparedStatement removeStatement = connection.prepareStatement(removeSqlQuery)) {
                removeStatement.setLong(1, key);
                removeStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }
}
