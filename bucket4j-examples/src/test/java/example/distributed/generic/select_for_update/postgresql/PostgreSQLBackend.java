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

package example.distributed.generic.select_for_update.postgresql;

import io.github.bucket4j.distributed.proxy.generic.select_for_update.AbstractSelectForUpdateBasedBackend;
import io.github.bucket4j.distributed.proxy.generic.select_for_update.SelectForUpdateBasedTransaction;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

public class PostgreSQLBackend extends AbstractSelectForUpdateBasedBackend<Long> {

    private static final String INIT_TABLE_SCRIPT =
            "CREATE TABLE IF NOT EXISTS buckets(" +
            " id    BIGINT    PRIMARY KEY,     " +
            " state BYTEA                      " +
            ");";

    private final DataSource dataSource;

    public PostgreSQLBackend(DataSource dataSource) throws SQLException {
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
                String selectForUpdateSQL = "SELECT state FROM buckets WHERE id = ? FOR UPDATE";
                try (PreparedStatement selectStatement = connection.prepareStatement(selectForUpdateSQL)) {
                    selectStatement.setLong(1, key);
                    try (ResultSet rs = selectStatement.executeQuery()) {
                        if (rs.next()) {
                            byte[] bucketState = rs.getBytes("state");
                            return Optional.ofNullable(bucketState);
                        }
                    }
                }

                // there are not persisted data for this bucket, so there is no raw which can act as lock
                String insertSQL = "INSERT INTO buckets(id, state) VALUES(?, null) ON CONFLICT(id) DO NOTHING";
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL)) {
                    insertStatement.setLong(1, key);
                    int rowsModified = insertStatement.executeUpdate();
                    if (rowsModified > 0) {
                        System.out.println(Thread.currentThread().getName() + " created new bucket key=" + key);
                    } else {
                        System.out.println(Thread.currentThread().getName() + " did not create new bucket key=" + key + " because it already created by other parallel thread");
                    }
                }

                // it is need to execute select for update again in order to obtain the lock
                try (PreparedStatement selectStatement = connection.prepareStatement(selectForUpdateSQL)) {
                    selectStatement.setLong(1, key);
                    try (ResultSet rs = selectStatement.executeQuery()) {
                        if (!rs.next()) {
                            // query does not see the record which inserted on step above
                            throw new IllegalStateException("Something unexpected happen, it needs to read PostgreSQL manual");
                        }
                        // we need to return epmty Optional because bucket is not initialized yet
                        return Optional.empty();
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
                String updateSQL = "UPDATE buckets SET state=? WHERE id=?";
                try (PreparedStatement updateStatement = connection.prepareStatement(updateSQL)) {
                    updateStatement.setBytes(1, data);
                    updateStatement.setLong(2, key);
                    updateStatement.executeUpdate();
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
