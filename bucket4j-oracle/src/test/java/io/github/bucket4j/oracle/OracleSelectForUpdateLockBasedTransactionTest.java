package io.github.bucket4j.oracle;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.OracleContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class OracleSelectForUpdateLockBasedTransactionTest extends AbstractDistributedBucketTest {

    private static OracleContainer container;
    private static DataSource dataSource;

    @BeforeAll
    public static void initializeInstance() throws SQLException {
        container = startOracleXeContainer();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings = BucketTableSettings.getDefault();
        final String INIT_TABLE_SCRIPT = "CREATE TABLE {0} ( {1} NUMBER NOT NULL PRIMARY KEY, {2} RAW(255) )";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT, tableSettings.getTableName(), tableSettings.getIdName(), tableSettings.getStateName());
                statement.execute(query);
            }
        }
        SQLProxyConfiguration<Long> configuration = SQLProxyConfiguration.builder()
                .withTableSettings(tableSettings)
                .build(dataSource);

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "OracleSelectForUpdateBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                new OracleSelectForUpdateBasedProxyManager<>(configuration)
            ),
            new ProxyManagerSpec<>(
                "OracleSelectForUpdateBasedProxyManager_withTimeout",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                new OracleSelectForUpdateBasedProxyManager<>(configuration, ClientSideConfig.getDefault().withRequestTimeout(Duration.ofSeconds(3)))
            )
        );
    }

    @AfterAll
    public static void shutdown() {
        if (container != null) {
            container.stop();
        }
    }

    private static DataSource createJdbcDataSource(OracleContainer container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setMaximumPoolSize(100);
        return new HikariDataSource(hikariConfig);
    }

    private static OracleContainer startOracleXeContainer() {
        OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
                .withDatabaseName("testDB")
                .withUsername("testUser")
                .withPassword("testPassword");
        oracle.start();
        return oracle;
    }

}
