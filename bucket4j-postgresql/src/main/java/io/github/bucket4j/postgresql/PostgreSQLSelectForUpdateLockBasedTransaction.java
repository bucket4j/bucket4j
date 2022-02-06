package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;

public class PostgreSQLSelectForUpdateLockBasedTransaction implements AbstractPostgreSQLLockBasedTransaction {

    private final long key;
    private final Connection connection;
    private final PostgreSQLProxyConfiguration configuration;
    private final String updateSqlQuery;
    private final String insertSqlQuery;
    private final String selectSqlQuery;

    PostgreSQLSelectForUpdateLockBasedTransaction(long key, PostgreSQLProxyConfiguration configuration, Connection connection) {
        this.key = key;
        this.configuration = configuration;
        this.connection = connection;
        updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=? WHERE {2}=?", configuration.getTableName(), configuration.getStateName(), configuration.getIdName());
        insertSqlQuery = MessageFormat.format("INSERT INTO {0}({1}, {2}) VALUES(?, null) ON CONFLICT({3}) DO NOTHING",
                configuration.getTableName(), configuration.getIdName(), configuration.getStateName(), configuration.getIdName());
        selectSqlQuery = MessageFormat.format("SELECT {0} FROM {1} WHERE {2} = ? FOR UPDATE", configuration.getStateName(), configuration.getTableName(), configuration.getIdName());
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void begin() {
        try {
            connection.setAutoCommit(false);
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
                        //TODO fix state
                        byte[] bucketStateBeforeTransaction = rs.getBytes(configuration.getStateName());
                        if (bucketStateBeforeTransaction != null) {
                            return bucketStateBeforeTransaction;
                        } else {
                            // we detected fake data that inserted by previous transaction
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
                        throw new IllegalStateException("Something unexpected happens, it needs to read the PostgreSQL manual");
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
        // do nothing, because locked rows will be auto unlocked when transaction finishes
    }
}
