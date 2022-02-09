package io.github.bucket4j.mysql;

import io.github.bucket4j.BucketExceptions;
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
 * The extension of Bucket4j library addressed to support <a href="https://www.mysql.com">MySQL</a>
 * To start work with the MySQL extension you must create a table, which will include the possibility to work with buckets
 * In order to do this, your table should include the next columns: id as a PRIMARY KEY (BIGINT) and state (BYTEA)
 * To define column names, {@link MySQLProxyConfiguration} include {@link io.github.bucket4j.distributed.jdbc.BucketTableSettings} which takes settings for the table to work with Bucket4j
 * @see {@link MySQLProxyConfigurationBuilder} to get more information how to build {@link MySQLProxyConfiguration}
 */
public class MySQLProxyManager extends AbstractLockBasedProxyManager<Long> {

    private final DataSource dataSource;
    private final MySQLProxyConfiguration configuration;
    private final String removeSqlQuery;

    /**
     *
     * @param configuration {@link MySQLProxyConfiguration} configuration.
     */
    public MySQLProxyManager(MySQLProxyConfiguration configuration) {
        super(configuration.getClientSideConfig());
        this.dataSource = Objects.requireNonNull(configuration.getDataSource());
        this.configuration = configuration;
        this.removeSqlQuery = MessageFormat.format("DELETE FROM {0} WHERE {1} = ?", configuration.getTableName(), configuration.getIdName());
    }

    @Override
    protected LockBasedTransaction allocateTransaction(Long key) {
        try {
            return new MySQLSelectForUpdateLockBasedTransaction(key, configuration, dataSource.getConnection());
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

    @Override
    protected void releaseTransaction(LockBasedTransaction transaction) {
        try {
            ((MySQLSelectForUpdateLockBasedTransaction) transaction).getConnection().close();
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
