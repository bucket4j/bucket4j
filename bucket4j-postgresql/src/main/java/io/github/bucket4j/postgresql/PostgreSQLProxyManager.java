package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockBasedTransaction;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;

public class PostgreSQLProxyManager extends AbstractLockBasedProxyManager<Long> {

    private final DataSource dataSource;
    private final PostgreSQLProxyConfiguration configuration;

    public PostgreSQLProxyManager(PostgreSQLProxyConfiguration configuration) {
        super(configuration.getClientSideConfig());
        this.dataSource = Objects.requireNonNull(configuration.getDataSource());
        this.configuration = configuration;
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
        // TODO implement removal
        throw new UnsupportedOperationException();
    }
}
