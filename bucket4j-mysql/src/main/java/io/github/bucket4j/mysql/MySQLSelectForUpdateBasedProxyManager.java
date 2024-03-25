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
package io.github.bucket4j.mysql;

import com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException;
import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractSelectForUpdateBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockAndGetResult;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.SelectForUpdateBasedTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.mysql.Bucket4jMySQL.MySQLSelectForUpdateBasedProxyManagerBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.mysql.com/">MySQL</a>
 *
 * <p>This implementation solves transaction/concurrency related problems via "SELECT FOR UPDATE" SQL syntax.
 *
 * @param <K> type of primary key
 */
public class MySQLSelectForUpdateBasedProxyManager<K> extends AbstractSelectForUpdateBasedProxyManager<K> {

    private final DataSource dataSource;
    private final PrimaryKeyMapper<K> primaryKeyMapper;
    private final String removeSqlQuery;
    private final String updateSqlQuery;
    private final String insertSqlQuery;
    private final String selectSqlQuery;

    MySQLSelectForUpdateBasedProxyManager(MySQLSelectForUpdateBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.dataSource = builder.getDataSource();
        this.primaryKeyMapper = builder.getPrimaryKeyMapper();
        this.removeSqlQuery = MessageFormat.format("DELETE FROM {0} WHERE {1} = ?", builder.getTableName(), builder.getIdColumnName());
        updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=? WHERE {2}=?", builder.getTableName(), builder.getStateColumnName(), builder.getIdColumnName());
        insertSqlQuery = MessageFormat.format("INSERT IGNORE INTO {0}({1}, {2}) VALUES(?, null)",
            builder.getTableName(), builder.getIdColumnName(), builder.getStateColumnName());
        selectSqlQuery = MessageFormat.format("SELECT {0} as state FROM {1} WHERE {2} = ? FOR UPDATE", builder.getStateColumnName(), builder.getTableName(), builder.getIdColumnName());
    }

    /**
     * @deprecated use {@link Bucket4jMySQL#selectForUpdateBasedBuilder(DataSource)}
     */
    @Deprecated
    public MySQLSelectForUpdateBasedProxyManager(SQLProxyConfiguration<K> configuration) {
        super(configuration.getClientSideConfig());
        this.dataSource = Objects.requireNonNull(configuration.getDataSource());
        this.primaryKeyMapper = configuration.getPrimaryKeyMapper();
        this.removeSqlQuery = MessageFormat.format("DELETE FROM {0} WHERE {1} = ?", configuration.getTableName(), configuration.getIdName());
        updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=? WHERE {2}=?", configuration.getTableName(), configuration.getStateName(), configuration.getIdName());
        insertSqlQuery = MessageFormat.format("INSERT IGNORE INTO {0}({1}, {2}) VALUES(?, null)",
                configuration.getTableName(), configuration.getIdName(), configuration.getStateName());
        selectSqlQuery = MessageFormat.format("SELECT {0} as state FROM {1} WHERE {2} = ? FOR UPDATE", configuration.getStateName(), configuration.getTableName(), configuration.getIdName());
    }

    @Override
    protected SelectForUpdateBasedTransaction allocateTransaction(K key, Optional<Long> requestTimeoutNanos) {
        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }

        return new SelectForUpdateBasedTransaction() {
            @Override
            public void begin(Optional<Long> requestTimeoutNanos) {
                try {
                    connection.setAutoCommit(false);
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }

            @Override
            public void update(byte[] data, RemoteBucketState newState, Optional<Long> requestTimeoutNanos) {
                try {
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSqlQuery)) {
                        applyTimeout(updateStatement, requestTimeoutNanos);
                        updateStatement.setBytes(1, data);
                        primaryKeyMapper.set(updateStatement, 2, key);
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
            public void rollback() {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }

            @Override
            public void commit(Optional<Long> requestTimeoutNanos) {
                try {
                    connection.commit();
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }

            @Override
            public LockAndGetResult tryLockAndGet(Optional<Long> requestTimeoutNanos) {
                try (PreparedStatement selectStatement = connection.prepareStatement(selectSqlQuery)) {
                    applyTimeout(selectStatement, requestTimeoutNanos);
                    primaryKeyMapper.set(selectStatement, 1, key);
                    try (ResultSet rs = selectStatement.executeQuery()) {
                        if (!rs.next()) {
                            return LockAndGetResult.notLocked();
                        }
                        byte[] bucketStateBeforeTransaction = rs.getBytes("state");
                        return LockAndGetResult.locked(bucketStateBeforeTransaction);
                    }
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }

            @Override
            public boolean tryInsertEmptyData(Optional<Long> requestTimeoutNanos) {
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSqlQuery)) {
                    applyTimeout(insertStatement, requestTimeoutNanos);
                    primaryKeyMapper.set(insertStatement, 1, key);
                    insertStatement.executeUpdate();
                    return true;
                } catch (MySQLTransactionRollbackException conflict) {
                    // do nothing, because parallel transaction has been already inserted the row
                    return false;
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }

        };
    }

    @Override
    public void removeProxy(K key) {
        try (Connection connection = dataSource.getConnection()) {
            try(PreparedStatement removeStatement = connection.prepareStatement(removeSqlQuery)) {
                primaryKeyMapper.set(removeStatement, 1, key);
                removeStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

}
