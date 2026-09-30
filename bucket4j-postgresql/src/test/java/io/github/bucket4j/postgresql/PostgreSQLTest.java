package io.github.bucket4j.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PostgreSQLTest extends AbstractDistributedBucketTest {

    private static PostgreSQLContainer container;
    private static DataSource dataSource;

    @BeforeAll
    public static void initializeInstance() throws SQLException {
        container = startPostgreSQLContainer();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings_1 = BucketTableSettings.getDefault();
        final String INIT_TABLE_SCRIPT_1 = "CREATE TABLE IF NOT EXISTS {0}({1} BIGINT PRIMARY KEY, {2} BYTEA, expires_at BIGINT, explicit_lock BIGINT)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT_1, tableSettings_1.getTableName(), tableSettings_1.getIdName(), tableSettings_1.getStateName());
                statement.execute(query);
            }
        }

        BucketTableSettings tableSettings_2 = BucketTableSettings.customSettings("buckets_String_key", "id", "state");
        final String INIT_TABLE_SCRIPT_2 = "CREATE TABLE IF NOT EXISTS {0}({1} VARCHAR PRIMARY KEY, {2} BYTEA, expires_at BIGINT, explicit_lock BIGINT)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT_2, tableSettings_2.getTableName(), tableSettings_2.getIdName(), tableSettings_2.getStateName());
                statement.execute(query);
            }
        }

        BackwardCompatibilityStateCheckHelper<Long> backwardCompatibilityHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(Long key) {
                String query = "SELECT state FROM bucket WHERE id = ?";
                try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setLong(1, key);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new IllegalStateException("There is no row for key " + key + " in table bucket");
                        }
                        return resultSet.getBytes(1);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void setRawState(Long key, byte[] state) {
                String query = "UPDATE bucket SET state = ? WHERE id = ?";
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setBytes(1, state);
                    statement.setLong(2, key);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        BackwardCompatibilityStateCheckHelper<String> backwardCompatibilityHelperStringKeyTable = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(String key) {
                String query = "SELECT state FROM buckets_String_key WHERE id = ?";
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, key);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new IllegalStateException("There is no row for key " + key + " in table buckets_String_key");
                        }
                        return resultSet.getBytes(1);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void setRawState(String key, byte[] state) {
                String query = "UPDATE buckets_String_key SET state = ? WHERE id = ?";
                try (Connection connection = dataSource.getConnection();
                    PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setBytes(1, state);
                    statement.setString(2, key);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "PostgreSQLadvisoryLockBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jPostgreSQL.advisoryLockBasedBuilder(dataSource)
                    .table("bucket")
                    .idColumn("id")
                    .stateColumn("state")
            ).checkExpiration().checkStateBackwardCompatibility(backwardCompatibilityHelper),
            new ProxyManagerSpec<>(
                "PostgreSQLSelectForUpdateBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jPostgreSQL.selectForUpdateBasedBuilder(dataSource)
                    .table("bucket")
                    .idColumn("id")
                    .stateColumn("state")
            ).checkExpiration().checkStateBackwardCompatibility(backwardCompatibilityHelper),
            new ProxyManagerSpec<>(
                "PostgreSQLadvisoryLockBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jPostgreSQL.advisoryLockBasedBuilder(dataSource)
                    .table("buckets_String_key")
                    .idColumn("id")
                    .stateColumn("state")
                    .primaryKeyMapper(PrimaryKeyMapper.STRING)
            ).checkExpiration().checkStateBackwardCompatibility(backwardCompatibilityHelperStringKeyTable)
        );
    }

    @AfterAll
    public static void shutdown() {
        if (container != null) {
            container.stop();
        }
    }

    private static DataSource createJdbcDataSource(PostgreSQLContainer container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setMaximumPoolSize(100);
        return new HikariDataSource(hikariConfig);
    }

    private static PostgreSQLContainer startPostgreSQLContainer() {
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:9.6.12");
        container.start();
        return container;
    }
}
