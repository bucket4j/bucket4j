package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockBasedTransaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Objects;

public class PostgreSQLProxyManager extends AbstractLockBasedProxyManager<Long> {

    private final DataSource dataSource;
    private final PostgreSQLProxyConfiguration configuration;
    private final String removeSqlQuery;

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
