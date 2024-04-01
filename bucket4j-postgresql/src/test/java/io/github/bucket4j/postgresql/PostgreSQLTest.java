package io.github.bucket4j.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
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
        final String INIT_TABLE_SCRIPT_1 = "CREATE TABLE IF NOT EXISTS {0}({1} BIGINT PRIMARY KEY, {2} BYTEA)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT_1, tableSettings_1.getTableName(), tableSettings_1.getIdName(), tableSettings_1.getStateName());
                statement.execute(query);
            }
        }

        BucketTableSettings tableSettings_2 = BucketTableSettings.customSettings("buckets_String_key", "id", "state");
        final String INIT_TABLE_SCRIPT_2 = "CREATE TABLE IF NOT EXISTS {0}({1} VARCHAR PRIMARY KEY, {2} BYTEA)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT_2, tableSettings_2.getTableName(), tableSettings_2.getIdName(), tableSettings_2.getStateName());
                statement.execute(query);
            }
        }

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "PostgreSQLadvisoryLockBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jPostgreSQL.advisoryLockBasedBuilder(dataSource)
                    .table("bucket")
                    .idColumn("id")
                    .stateColumn("state")
            ),
            new ProxyManagerSpec<>(
                "PostgreSQLSelectForUpdateBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jPostgreSQL.selectForUpdateBasedBuilder(dataSource)
                    .table("bucket")
                    .idColumn("id")
                    .stateColumn("state")
            ),
            new ProxyManagerSpec<>(
                "PostgreSQLadvisoryLockBasedProxyManager_StringKey",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jPostgreSQL.advisoryLockBasedBuilder(dataSource)
                    .table("buckets_String_key")
                    .idColumn("id")
                    .stateColumn("state")
                    .primaryKeyMapper(PrimaryKeyMapper.STRING)
            )
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
        PostgreSQLContainer container = new PostgreSQLContainer();
        container.start();
        return container;
    }
}
