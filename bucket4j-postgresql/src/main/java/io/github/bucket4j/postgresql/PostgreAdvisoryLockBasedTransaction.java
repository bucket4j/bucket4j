package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockBasedTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgreAdvisoryLockBasedTransaction implements LockBasedTransaction {

    private final long key;
    private final Connection connection;
    private final PostgreSQLProxyConfiguration configuration;

    PostgreAdvisoryLockBasedTransaction(long key, PostgreSQLProxyConfiguration configuration, Connection connection) {
        this.key = key;
        this.configuration = configuration;
        this.connection = connection;
    }

    Connection getConnection() {
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
            // acquire pessimistic lock
            String lockSQL = "SELECT pg_advisory_xact_lock(?)";
            try (PreparedStatement lockStatement = connection.prepareStatement(lockSQL)) {
                lockStatement.setLong(1, key);
                //try (ResultSet rs = lockStatement.executeQuery()) {}
            }

            // select data if exists
            String selectSQL = "SELECT ? FROM ? WHERE ? = ?";
            try (PreparedStatement selectStatement = connection.prepareStatement(selectSQL)) {
                selectStatement.setString(1, configuration.getStateName());
                selectStatement.setString(2, configuration.getTableName());
                selectStatement.setString(3, configuration.getIdName());
                selectStatement.setLong(4, key);
                try (ResultSet rs = selectStatement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBytes(configuration.getStateName());
                    } else {
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

    @Override
    public void update(byte[] data) {
        try {
            String updateSQL = "UPDATE ? SET ?=? WHERE ?=?";
            try (PreparedStatement updateStatement = connection.prepareStatement(updateSQL)) {
                updateStatement.setString(1, configuration.getTableName());
                updateStatement.setString(2, configuration.getStateName());
                updateStatement.setBytes(3, data);
                updateStatement.setString(4, configuration.getIdName());
                updateStatement.setLong(5, key);
                updateStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

    @Override
    public void create(byte[] data) {
        try {
            String insertSQL = "INSERT INTO ?(?, ?) VALUES(?, ?)";
            try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL)) {
                insertStatement.setString(1, configuration.getTableName());
                insertStatement.setString(2, configuration.getIdName());
                insertStatement.setString(3, configuration.getStateName());
                insertStatement.setLong(4, key);
                insertStatement.setBytes(5, data);
                insertStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
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
        throw new UnsupportedOperationException();
    }
}
