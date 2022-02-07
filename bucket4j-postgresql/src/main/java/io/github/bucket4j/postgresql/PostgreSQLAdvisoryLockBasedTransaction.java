package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;

public class PostgreSQLAdvisoryLockBasedTransaction implements AbstractPostgreSQLLockBasedTransaction {

    private final long key;
    private final Connection connection;
    private final PostgreSQLProxyConfiguration configuration;
    private final String updateSqlQuery;
    private final String insertSqlQuery;
    private final String selectSqlQuery;

    PostgreSQLAdvisoryLockBasedTransaction(long key, PostgreSQLProxyConfiguration configuration, Connection connection) {
        this.key = key;
        this.configuration = configuration;
        this.connection = connection;
        updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=? WHERE {2}=?", configuration.getTableName(), configuration.getStateName(), configuration.getIdName());
        insertSqlQuery = MessageFormat.format("INSERT INTO {0}({1}, {2}) VALUES(?, ?)", configuration.getTableName(), configuration.getIdName(), configuration.getStateName());
        selectSqlQuery = MessageFormat.format("SELECT {0} FROM {1} WHERE {2} = ?", configuration.getStateName(), configuration.getTableName(), configuration.getIdName());
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
            String lockSQL = "SELECT pg_advisory_xact_lock(?)";
            try (PreparedStatement lockStatement = connection.prepareStatement(lockSQL)) {
                lockStatement.setLong(1, key);
                lockStatement.executeQuery();
            }

            try (PreparedStatement selectStatement = connection.prepareStatement(selectSqlQuery)) {
                selectStatement.setLong(1, key);
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
        try {
            try (PreparedStatement insertStatement = connection.prepareStatement(insertSqlQuery)) {
                insertStatement.setLong(1, key);
                insertStatement.setBytes(2, data);
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
    public void unlock() {}
}
