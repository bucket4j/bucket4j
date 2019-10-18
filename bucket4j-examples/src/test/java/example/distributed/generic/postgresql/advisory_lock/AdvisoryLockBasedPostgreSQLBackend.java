/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package example.distributed.generic.postgresql.advisory_lock;

import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractSelectForUpdateBasedBackend;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.SelectForUpdateBasedTransaction;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

public class AdvisoryLockBasedPostgreSQLBackend extends AbstractSelectForUpdateBasedBackend<Long> {

    private static final String INIT_TABLE_SCRIPT =
            "CREATE TABLE IF NOT EXISTS buckets(" +
            " id    BIGINT    PRIMARY KEY,     " +
            " state BYTEA                      " +
            ");";

    private final DataSource dataSource;

    public AdvisoryLockBasedPostgreSQLBackend(DataSource dataSource) throws SQLException {
        this.dataSource = dataSource;

        // TODO for real application table initialization should be moved to the right place
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(INIT_TABLE_SCRIPT);
            }
        }

    }

    @Override
    protected SelectForUpdateBasedTransaction allocateTransaction(Long key) {
        try {
            return new PostgreSelectForUpdateBasedTransaction(key, dataSource.getConnection());
        } catch (SQLException e) {
            // TODO implement logging here
            e.printStackTrace();

            // TODO use appropriate type of exception
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void releaseTransaction(SelectForUpdateBasedTransaction transaction) {
        try {
            // return connection to pool
            ((PostgreSelectForUpdateBasedTransaction) transaction).connection.close();
        } catch (SQLException e) {
            // TODO implement logging here
            e.printStackTrace();

            // TODO use appropriate type of exception
            throw new IllegalStateException(e);
        }
    }

    private class PostgreSelectForUpdateBasedTransaction implements SelectForUpdateBasedTransaction {

        private final long key;
        private final Connection connection;

        private boolean bucketWasPersistedBeforeTransaction = false;

        private PostgreSelectForUpdateBasedTransaction(long key, Connection connection) {
            this.key = key;
            this.connection = connection;
        }

        @Override
        public void begin() {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException e) {
                // TODO implement logging here
                e.printStackTrace();

                // TODO use appropriate type of exception
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Optional<byte[]> lockAndGet() {
            try {
                // acquire pessimistic lock
                String lockSQL = "SELECT pg_advisory_xact_lock(?)";
                try (PreparedStatement lockStatement = connection.prepareStatement(lockSQL)) {
                    lockStatement.setLong(1, key);
                    try (ResultSet rs = lockStatement.executeQuery()) {}
                }

                // select data if exists
                String selectSQL = "SELECT state FROM buckets WHERE id = ?";
                try (PreparedStatement selectStatement = connection.prepareStatement(selectSQL)) {
                    selectStatement.setLong(1, key);
                    try (ResultSet rs = selectStatement.executeQuery()) {
                        if (rs.next()) {
                            byte[] bucketState = rs.getBytes("state");
                            bucketWasPersistedBeforeTransaction = true;
                            return Optional.of(bucketState);
                        } else {
                            return Optional.empty();
                        }
                    }
                }
            } catch (SQLException e) {
                // TODO implement logging here
                e.printStackTrace();

                // TODO use appropriate type of exception
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void update(byte[] data) {
            try {
                if (bucketWasPersistedBeforeTransaction) {
                    String updateSQL = "UPDATE buckets SET state=? WHERE id=?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSQL)) {
                        updateStatement.setBytes(1, data);
                        updateStatement.setLong(2, key);
                        updateStatement.executeUpdate();
                    }
                } else {
                    String insertSQL = "INSERT INTO buckets(id, state) VALUES(?, ?)";
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL)) {
                        insertStatement.setLong(1, key);
                        insertStatement.setBytes(2, data);
                        insertStatement.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                // TODO implement logging here
                e.printStackTrace();

                // TODO use appropriate type of exception
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void rollback() {
            try {
                connection.rollback();
            } catch (SQLException e) {
                // TODO implement logging here
                e.printStackTrace();

                // TODO use appropriate type of exception
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void commit() {
            try {
                connection.commit();
            } catch (SQLException e) {
                // TODO implement logging here
                e.printStackTrace();

                // TODO use appropriate type of exception
                throw new IllegalStateException(e);
            }
        }

    }

}
