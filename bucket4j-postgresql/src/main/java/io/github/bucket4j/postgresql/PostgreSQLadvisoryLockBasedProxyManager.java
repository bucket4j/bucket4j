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
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.jdbc.LockIdSupplier;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.LockBasedTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL.PostgreSQLAdvisoryLockBasedProxyManagerBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The extension of Bucket4j library addressed to support <a href="https://www.postgresql.org/">PostgreSQL</a>
 *
 * <p>This implementation solves transaction/concurrency related problems via pg_advisory_xact_lock
 *
 * @param <K> type of primary key
 */
public class PostgreSQLadvisoryLockBasedProxyManager<K> extends AbstractLockBasedProxyManager<K> implements ExpiredEntriesCleaner {

    private final LockIdSupplier<K> lockIdSupplier;
    private final PrimaryKeyMapper<K> primaryKeyMapper;
    private final DataSource dataSource;
    private final String removeSqlQuery;
    private final String updateSqlQuery;
    private final String insertSqlQuery;
    private final String selectSqlQuery;
    private final String clearExpiredSqlQuery;
    private final List<CustomColumnProvider<K>> customColumns = new ArrayList<>();

    PostgreSQLadvisoryLockBasedProxyManager(PostgreSQLAdvisoryLockBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.dataSource = builder.getDataSource();
        this.primaryKeyMapper = builder.getPrimaryKeyMapper();
        this.lockIdSupplier = builder.getLockIdSupplier();
        this.removeSqlQuery = MessageFormat.format("DELETE FROM {0} WHERE {1} = ?", builder.getTableName(), builder.getIdColumnName());
        this.selectSqlQuery = MessageFormat.format("SELECT {0} as state FROM {1} WHERE {2} = ?", builder.getStateColumnName(), builder.getTableName(), builder.getIdColumnName());
        this.customColumns.addAll(builder.getCustomColumns());
        getClientSideConfig().getExpirationAfterWriteStrategy().ifPresent(expiration -> {
            this.customColumns.add(CustomColumnProvider.createExpiresInColumnProvider(builder.getExpiresAtColumnName(), expiration));
            String lockColumn = builder.getLockColumn();
            this.customColumns.add(new CustomColumnProvider<>() {
                @Override
                public void setCustomField(K key, int paramIndex, PreparedStatement statement, RemoteBucketState state, long currentTimeNanos) throws SQLException {
                    statement.setLong(paramIndex, lockIdSupplier.toLockId(key));
                }

                @Override
                public String getCustomFieldName() {
                    return lockColumn;
                }
            });
        });
        if (customColumns.isEmpty()) {
            this.insertSqlQuery = MessageFormat.format("INSERT INTO {0}({1}, {2}) VALUES(?, ?)", builder.getTableName(), builder.getIdColumnName(), builder.getStateColumnName());
            this.updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=? WHERE {2}=?", builder.getTableName(), builder.getStateColumnName(), builder.getIdColumnName());
        } else {
            String customPartInUpdate = String.join(",", customColumns.stream().map(column -> column.getCustomFieldName() + "=?").toList());
            this.updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=?,{2} WHERE {3}=?", builder.getTableName(), builder.getStateColumnName(), customPartInUpdate, builder.getIdColumnName());

            String customInsertColumns = String.join(",", customColumns.stream().map(CustomColumnProvider::getCustomFieldName).toList());
            String customInsertValues = String.join(",", customColumns.stream().map(column -> "?").toList());
            this.insertSqlQuery = MessageFormat.format("INSERT INTO {0}({1},{2},{3}) VALUES(?,?,{4})",
                builder.getTableName(),
                builder.getIdColumnName(),
                builder.getStateColumnName(),
                customInsertColumns,
                customInsertValues
            );
        }
        this.clearExpiredSqlQuery = MessageFormat.format(
            """
            DELETE FROM {0} WHERE
                {2} < ? AND
                {1} IN(SELECT {1} FROM {0} WHERE {2} < ? AND pg_try_advisory_xact_lock({3}) LIMIT ?)
            """, builder.getTableName(), builder.getIdColumnName(), builder.getExpiresAtColumnName(), builder.getLockColumn()
        );
    }

    /**
     * @deprecated use {@link Bucket4jPostgreSQL#advisoryLockBasedBuilder(DataSource)}
     */
    @Deprecated
    public PostgreSQLadvisoryLockBasedProxyManager(SQLProxyConfiguration<K> configuration) {
        super(configuration.getClientSideConfig());
        this.clearExpiredSqlQuery = null;
        this.dataSource = Objects.requireNonNull(configuration.getDataSource());
        this.primaryKeyMapper = configuration.getPrimaryKeyMapper();
        this.lockIdSupplier = (LockIdSupplier) LockIdSupplier.DEFAULT;
        this.removeSqlQuery = MessageFormat.format("DELETE FROM {0} WHERE {1} = ?", configuration.getTableName(), configuration.getIdName());
        this.updateSqlQuery = MessageFormat.format("UPDATE {0} SET {1}=? WHERE {2}=?", configuration.getTableName(), configuration.getStateName(), configuration.getIdName());
        this.insertSqlQuery = MessageFormat.format("INSERT INTO {0}({1}, {2}) VALUES(?, ?)", configuration.getTableName(), configuration.getIdName(), configuration.getStateName());
        this.selectSqlQuery = MessageFormat.format("SELECT {0} as state FROM {1} WHERE {2} = ?", configuration.getStateName(), configuration.getTableName(), configuration.getIdName());
        if (getClientSideConfig().getExpirationAfterWriteStrategy().isPresent()) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected LockBasedTransaction allocateTransaction(K key, Optional<Long> requestTimeout) {
        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }

        return new LockBasedTransaction() {
            @Override
            public void begin(Optional<Long> requestTimeout) {
                try {
                    connection.setAutoCommit(false);
                } catch (SQLException e) {
                    throw new BucketExceptions.BucketExecutionException(e);
                }
            }

            @Override
            public byte[] lockAndGet(Optional<Long> requestTimeout) {
                try {
                    String lockSQL = "SELECT pg_advisory_xact_lock(?)";
                    try (PreparedStatement lockStatement = connection.prepareStatement(lockSQL)) {
                        applyTimeout(lockStatement, requestTimeout);
                        long advisoryLockValue = lockIdSupplier.toLockId(key);
                        lockStatement.setLong(1, advisoryLockValue);
                        lockStatement.executeQuery();
                    }

                    try (PreparedStatement selectStatement = connection.prepareStatement(selectSqlQuery)) {
                        primaryKeyMapper.set(selectStatement, 1, key);
                        try (ResultSet rs = selectStatement.executeQuery()) {
                            if (rs.next()) {
                                return rs.getBytes("state");
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
            public void update(byte[] data, RemoteBucketState newState, Optional<Long> requestTimeout) {
                try {
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSqlQuery)) {
                        applyTimeout(updateStatement, requestTimeout);
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

            @Override
            public void create(byte[] data, RemoteBucketState newState, Optional<Long> requestTimeout) {
                try {
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertSqlQuery)) {
                        applyTimeout(insertStatement, requestTimeout);
                        int i = 0;
                        primaryKeyMapper.set(insertStatement, ++i, key);
                        insertStatement.setBytes(++i, data);
                        for (CustomColumnProvider<K> column : customColumns) {
                            column.setCustomField(key, ++i, insertStatement, newState, currentTimeNanos());
                        }
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
            public void commit(Optional<Long> requestTimeout) {
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
                primaryKeyMapper.set(removeStatement, 1, key);
                removeStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
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
