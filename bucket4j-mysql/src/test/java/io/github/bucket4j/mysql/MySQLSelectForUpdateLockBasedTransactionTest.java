package io.github.bucket4j.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

public class MySQLSelectForUpdateLockBasedTransactionTest extends AbstractDistributedBucketTest {

    private static MySQLContainer container;
    private static DataSource dataSource;

    @BeforeAll
    public static void initializeInstance() throws SQLException {
        container = startMySQLContainer();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings = BucketTableSettings.customSettings("test.bucket", "id", "state");
        final String INIT_TABLE_SCRIPT = "CREATE TABLE IF NOT EXISTS {0}({1} BIGINT PRIMARY KEY, {2} BLOB, expires_at BIGINT)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT, tableSettings.getTableName(), tableSettings.getIdName(), tableSettings.getStateName());
                statement.execute(query);
            }
        }

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "MySQLSelectForUpdateBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jMySQL.selectForUpdateBasedBuilder(dataSource)
            ).checkExpiration()
        );
    }

    @AfterAll
    public static void shutdown() {
        if (container != null) {
            container.stop();
        }
    }

    private static DataSource createJdbcDataSource(MySQLContainer container) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(container.getJdbcUrl());
        config.setUsername(container.getUsername());
        config.setPassword(container.getPassword());
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    private static MySQLContainer startMySQLContainer() {
        MySQLContainer container = new MySQLContainer(DockerImageName.parse("mysql:8.0.36"));
        container.start();
        return container;
    }

}
