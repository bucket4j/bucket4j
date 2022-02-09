package io.github.bucket4j.mysql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.LockBasedTransaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private final String updateSqlQuery;
    private final String insertSqlQuery;
    private final String selectSqlQuery;

    /**
     *
     * @param configuration {@link MySQLProxyConfiguration} configuration.
     */
    public MySQLProxyManager(MySQLProxyConfiguration configuration) {
        super(configuration.getClientSideConfig());
        this.dataSource = Objects.requireNonNull(configuration.getDataSource());
        this.configuration = configuration;
        this.removeSqlQuery = MessageFormat.format("DELETE FROM {0} WHERE {1} = ?", configuration.getTableName(), configuration.getIdName());
        updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=? WHERE {2}=?", configuration.getTableName(), configuration.getStateName(), configuration.getIdName());
        insertSqlQuery = MessageFormat.format("INSERT IGNORE INTO {0}({1}, {2}) VALUES(?, null)",
                configuration.getTableName(), configuration.getIdName(), configuration.getStateName(), configuration.getIdName());
        selectSqlQuery = MessageFormat.format("SELECT {0} FROM {1} WHERE {2} = ? FOR UPDATE", configuration.getStateName(), configuration.getTableName(), configuration.getIdName());
    }

    @Override
    protected LockBasedTransaction allocateTransaction(Long key) {
        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }

        return new LockBasedTransaction() {
            @Override
            public void begin() {
                try {
                    connection.setAutoCommit(false);
                    PreparedStatement sth = connection.prepareStatement("SELECT GET_LOCK(?, ?)");
                    sth.setLong(1, key);
                    sth.setInt(2, 2);
                    sth.executeQuery();
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }
            @Override
            public byte[] lockAndGet() {
                try {
                    try (PreparedStatement selectStatement = connection.prepareStatement(selectSqlQuery)) {
                        selectStatement.setLong(1, key);
                        try (ResultSet rs = selectStatement.executeQuery()) {
                            if (rs.next()) {
                                byte[] bucketStateBeforeTransaction = rs.getBytes(configuration.getStateName());
                                if (bucketStateBeforeTransaction != null) {
                                    return bucketStateBeforeTransaction;
                                } else {
                                    return null;
                                }
                            }
                        }
                    }
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertSqlQuery)) {
                        insertStatement.setLong(1, key);
                        insertStatement.executeUpdate();
                    }
                    try (PreparedStatement selectStatement = connection.prepareStatement(selectSqlQuery)) {
                        selectStatement.setLong(1, key);
                        try (ResultSet rs = selectStatement.executeQuery()) {
                            if (!rs.next()) {
                                throw new IllegalStateException("Something unexpected happens, it needs to read the MySQL manual");
                            }
                            return null;
                        }
                    }
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }
            @Override
            public void update(byte[] data) {
                try {
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSqlQuery)) {
                        updateStatement.setBytes(1, data);
                        updateStatement.setLong(2, key);
                        updateStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }
            @Override
            public void release() {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }
            @Override
            public void create(byte[] data) {
                update(data);
            }
            @Override
            public void rollback() {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }
            @Override
            public void commit() {
                try {
                    connection.commit();
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }
            @Override
            public void unlock() {
                try {
                    PreparedStatement sth = connection.prepareStatement("SELECT RELEASE_LOCK(?)");
                    sth.setLong(1, key);
                    sth.executeQuery();
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }
        };
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
