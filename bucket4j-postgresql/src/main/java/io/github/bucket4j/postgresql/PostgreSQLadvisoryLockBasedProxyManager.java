/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.postgresql;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfigurationBuilder;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.LockBasedTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Objects;

/**
 * @author Maxim Bartkov
 * The extension of Bucket4j library addressed to support <a href="https://www.postgresql.org/">PostgreSQL</a>
 * To start work with the PostgreSQL extension you must create a table, which will include the possibility to work with buckets
 * In order to do this, your table should include the next columns: id as a PRIMARY KEY (BIGINT) and state (BYTEA)
 * To define column names, {@link SQLProxyConfiguration} include {@link BucketTableSettings} which takes settings for the table to work with Bucket4j.
 *
 * <p>This implementation solves transaction related problems via pg_advisory_xact_lock
 * locks an application-defined resource, which can be identified either by a single 64-bit key value or two 32-bit key values (note that these two key spaces do not overlap).
 * If another session already holds a lock on the same resource identifier, this function will wait until the resource becomes available.
 * The lock is exclusive.
 * Multiple lock requests stack so that if the same resource is locked three times it must then be unlocked three times to be released for other sessions use.
 * The lock is automatically released at the end of the current transaction and cannot be released explicitly.
 * @see {@link SQLProxyConfigurationBuilder} to get more information how to build {@link SQLProxyConfiguration}
 *
 * @param <K> type of primary key
 */
public class PostgreSQLadvisoryLockBasedProxyManager<K> extends AbstractLockBasedProxyManager<K> {

    private final DataSource dataSource;
    private final SQLProxyConfiguration<K> configuration;
    private final String removeSqlQuery;
    private final String updateSqlQuery;
    private final String insertSqlQuery;
    private final String selectSqlQuery;

    /**
     *
     * @param configuration {@link SQLProxyConfiguration} configuration.
     */
    public <T extends Object> PostgreSQLadvisoryLockBasedProxyManager(SQLProxyConfiguration<K> configuration) {
        super(configuration.getClientSideConfig());
        this.dataSource = Objects.requireNonNull(configuration.getDataSource());
        this.configuration = configuration;
        this.removeSqlQuery = MessageFormat.format("DELETE FROM {0} WHERE {1} = ?", configuration.getTableName(), configuration.getIdName());
        this.updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=? WHERE {2}=?", configuration.getTableName(), configuration.getStateName(), configuration.getIdName());
        this.insertSqlQuery = MessageFormat.format("INSERT INTO {0}({1}, {2}) VALUES(?, ?)", configuration.getTableName(), configuration.getIdName(), configuration.getStateName());
        this.selectSqlQuery = MessageFormat.format("SELECT {0} FROM {1} WHERE {2} = ?", configuration.getStateName(), configuration.getTableName(), configuration.getIdName());
    }

    @Override
    protected LockBasedTransaction allocateTransaction(K key) {
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
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }

            @Override
            public byte[] lockAndGet() {
                try {
                    String lockSQL = "SELECT pg_advisory_xact_lock(?)";
                    try (PreparedStatement lockStatement = connection.prepareStatement(lockSQL)) {
                        long advisoryLockValue = (key instanceof Number) ? ((Number) key).longValue(): key.hashCode();
                        lockStatement.setLong(1, advisoryLockValue);
                        lockStatement.executeQuery();
                    }

                    try (PreparedStatement selectStatement = connection.prepareStatement(selectSqlQuery)) {
                        configuration.getPrimaryKeyMapper().set(selectStatement, 1, key);
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
            public void update(byte[] data, RemoteBucketState newState) {
                try {
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSqlQuery)) {
                        updateStatement.setBytes(1, data);
                        configuration.getPrimaryKeyMapper().set(updateStatement, 2, key);
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
            public void create(byte[] data, RemoteBucketState newState) {
                try {
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertSqlQuery)) {
                        configuration.getPrimaryKeyMapper().set(insertStatement, 1, key);
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
            public void unlock() {
                // advisory lock implicitly unlocked on commit/rollback
            }
        };
    }

    @Override
    public void removeProxy(K key) {
        try (Connection connection = dataSource.getConnection()) {
            try(PreparedStatement removeStatement = connection.prepareStatement(removeSqlQuery)) {
                configuration.getPrimaryKeyMapper().set(removeStatement, 1, key);
                removeStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

}
