package io.github.bucket4j.db2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Db2Container;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Db2Test extends AbstractDistributedBucketTest {

    private static Db2Container container;
    private static DataSource dataSource;

    @BeforeAll
    public static void initializeInstance() throws SQLException {
        container = startDb2Container();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings_1 = BucketTableSettings.getDefault();
        final String INIT_TABLE_SCRIPT_1 = "CREATE TABLE IF NOT EXISTS {0}({1} BIGINT NOT NULL PRIMARY KEY, {2} VARCHAR(512), expires_at BIGINT)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT_1, tableSettings_1.getTableName(), tableSettings_1.getIdName(), tableSettings_1.getStateName());
                statement.execute(query);
            }
        }

        BucketTableSettings tableSettings_2 = BucketTableSettings.customSettings("buckets_String_key", "id", "state");
        final String INIT_TABLE_SCRIPT_2 = "CREATE TABLE IF NOT EXISTS {0}({1} VARCHAR(64) NOT NULL PRIMARY KEY, {2} VARCHAR(512), expires_at BIGINT)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT_2, tableSettings_2.getTableName(), tableSettings_2.getIdName(), tableSettings_2.getStateName());
                statement.execute(query);
            }
        }

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "Db2SelectForUpdateBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jDb2.selectForUpdateBasedBuilder(dataSource)
                    .table("bucket")
                    .idColumn("id")
                    .stateColumn("state")
            ).checkExpiration(),
            new ProxyManagerSpec<>(
                "Db2SelectForUpdateBasedProxyManager_StringKey",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jDb2.selectForUpdateBasedBuilder(dataSource)
                    .table("buckets_String_key")
                    .idColumn("id")
                    .stateColumn("state")
            ).checkExpiration()
        );
    }

    @AfterAll
    public static void shutdown() {
        if (container != null) {
            container.stop();
        }
    }

    private static DataSource createJdbcDataSource(Db2Container container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setMaximumPoolSize(100);
        return new HikariDataSource(hikariConfig);
    }

    private static Db2Container startDb2Container() {
        Db2Container container = new Db2Container().acceptLicense();
        container.start();
        return container;
    }
}
