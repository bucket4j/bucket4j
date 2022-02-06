package io.github.bucket4j.postgresql;

import java.sql.Connection;

public final class PostgreSQLLockBasedTransactionFactory {

    private final long key;
    private final Connection connection;
    private final PostgreSQLProxyConfiguration configuration;

    public PostgreSQLLockBasedTransactionFactory(long key, PostgreSQLProxyConfiguration configuration, Connection connection) {
        this.key = key;
        this.connection = connection;
        this.configuration = configuration;
    }

    public AbstractPostgreSQLLockBasedTransaction getLockBasedTransaction(LockBasedTransactionType type) {
        if(LockBasedTransactionType.ADVISORY.equals(type)) {
            return new PostgreSQLAdvisoryLockBasedTransaction(key, configuration, connection);
        } else if (LockBasedTransactionType.SELECT_FOR_UPDATE.equals(type)) {
            return new PostgreSQLSelectForUpdateLockBasedTransaction(key, configuration, connection);
        }
        throw new UnsupportedLockBasedTransactionException(type);
    }
}
