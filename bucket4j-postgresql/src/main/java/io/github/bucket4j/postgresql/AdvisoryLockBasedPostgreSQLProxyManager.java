package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockBasedTransaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class AdvisoryLockBasedPostgreSQLProxyManager extends AbstractLockBasedProxyManager<Long> {

    private final DataSource dataSource;
    private final PostgreSQLProxyConfiguration configuration;
    private static final String INIT_TABLE_SCRIPT = "CREATE TABLE IF NOT EXISTS ?(? BIGINT PRIMARY KEY, ? BYTEA);";

    public AdvisoryLockBasedPostgreSQLProxyManager(PostgreSQLProxyConfiguration configuration) throws SQLException {
        super(configuration.getClientSideConfig());
        this.dataSource = Objects.requireNonNull(configuration.getDataSource());
        this.configuration = configuration;

        // TODO for real application table initialization should be moved to the right place
        // TODO ANSWER for above TODO - not sure about that statement, I've no idea, where a better place is than here
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(INIT_TABLE_SCRIPT)) {
                statement.setString(1, configuration.getTableName());
                statement.setString(2, configuration.getIdName());
                statement.setString(3, configuration.getTableName());
                statement.executeQuery();
            }
        }
    }

    @Override
    protected LockBasedTransaction allocateTransaction(Long key) {
        try {
            return new PostgreAdvisoryLockBasedTransaction(key, configuration, dataSource.getConnection());
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

    @Override
    protected void releaseTransaction(LockBasedTransaction transaction) {
        try {
            // return connection to pool
            ((PostgreAdvisoryLockBasedTransaction) transaction).getConnection().close();
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
