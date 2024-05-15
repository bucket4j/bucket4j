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
import io.github.bucket4j.distributed.jdbc.CustomColumnProvider;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractSelectForUpdateBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.LockAndGetResult;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.SelectForUpdateBasedTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL.PostgreSQLSelectForUpdateBasedProxyManagerBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.postgresql.org/">PostgreSQL</a>
 *
 * <p>This implementation solves transaction/concurrency related problems via "SELECT FOR UPDATE" SQL syntax.
 *
 * @param <K> type of primary key
 */
public class PostgreSQLSelectForUpdateBasedProxyManager<K> extends AbstractSelectForUpdateBasedProxyManager<K> implements ExpiredEntriesCleaner {

    private final DataSource dataSource;
    private final PrimaryKeyMapper<K> primaryKeyMapper;
    private final String removeSqlQuery;
    private final String updateSqlQuery;
    private final String insertSqlQuery;
    private final String selectSqlQuery;
    private final String clearExpiredSqlQuery;
    private final List<CustomColumnProvider<K>> customColumns = new ArrayList<>();

    public PostgreSQLSelectForUpdateBasedProxyManager(PostgreSQLSelectForUpdateBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.dataSource = builder.getDataSource();
        this.primaryKeyMapper = builder.getPrimaryKeyMapper();
        this.removeSqlQuery = MessageFormat.format("DELETE FROM {0} WHERE {1} = ?", builder.getTableName(), builder.getIdColumnName());
        this.insertSqlQuery = MessageFormat.format("INSERT INTO {0}({1}, {2}) VALUES(?, null) ON CONFLICT({3}) DO NOTHING",
            builder.getTableName(), builder.getIdColumnName(), builder.getStateColumnName(), builder.getIdColumnName());
        this.selectSqlQuery = MessageFormat.format("SELECT {0} as state FROM {1} WHERE {2} = ? FOR UPDATE", builder.getStateColumnName(), builder.getTableName(), builder.getIdColumnName());
        this.customColumns.addAll(builder.getCustomColumns());
        getClientSideConfig().getExpirationAfterWriteStrategy().ifPresent(expiration -> {
            this.customColumns.add(CustomColumnProvider.createExpiresInColumnProvider(builder.getExpiresAtColumnName(), expiration));
        });
        if (customColumns.isEmpty()) {
            this.updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=? WHERE {2}=?", builder.getTableName(), builder.getStateColumnName(), builder.getIdColumnName());
        } else {
            String customPartInUpdate = String.join(",", customColumns.stream().map(column -> column.getCustomFieldName() + "=?").toList());
            this.updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=?,{2} WHERE {3}=?", builder.getTableName(), builder.getStateColumnName(), customPartInUpdate, builder.getIdColumnName());
        }
        this.clearExpiredSqlQuery = MessageFormat.format(
            """
            DELETE FROM {0} WHERE
                {2} < ? AND
                {1} IN(SELECT {1} FROM {0} WHERE {2} < ? LIMIT ? FOR UPDATE SKIP LOCKED)
            """, builder.getTableName(), builder.getIdColumnName(), builder.getExpiresAtColumnName()
        );
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
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
                        if (rs.next()) {
                            byte[] data = rs.getBytes("state");
                            return LockAndGetResult.locked(data);
                        } else {
                            return LockAndGetResult.notLocked();
                        }
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
                    return insertStatement.executeUpdate() > 0;
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }

            @Override
            public void update(byte[] data, RemoteBucketState newState, Optional<Long> requestTimeoutNanos) {
                try {
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSqlQuery)) {
                        applyTimeout(updateStatement, requestTimeoutNanos);
                        int i = 0;
                        updateStatement.setBytes(++i, data);
                        for (CustomColumnProvider<K> column : customColumns) {
                            column.setCustomField(key, ++i, updateStatement, newState, currentTimeNanos());
                        }
                        primaryKeyMapper.set(updateStatement, ++i, key);
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

    @Override
    public int removeExpired(int batchSize) {
        try (Connection connection = dataSource.getConnection()) {
            long currentTimeMillis = System.currentTimeMillis();
            try(PreparedStatement clearStatement = connection.prepareStatement(clearExpiredSqlQuery)) {
                clearStatement.setLong(1, currentTimeMillis);
                clearStatement.setLong(2, currentTimeMillis);
                clearStatement.setInt(3, batchSize);
                return clearStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

}
