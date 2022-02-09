package io.github.bucket4j.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostgreSQLAdvisoryLockBasedTransactionTest extends AbstractDistributedBucketTest<Long> {

    private static PostgreSQLContainer container;
    private static DataSource dataSource;
    private static PostgreSQLadvisoryLockBasedProxyManager proxyManager;

    @BeforeClass
    public static void initializeInstance() throws SQLException {
        container = startPostgreSQLContainer();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings = BucketTableSettings.getDefault();
        final String INIT_TABLE_SCRIPT = "CREATE TABLE IF NOT EXISTS {0}({1} BIGINT PRIMARY KEY, {2} BYTEA)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT, tableSettings.getTableName(), tableSettings.getIdName(), tableSettings.getStateName());
                statement.execute(query);
            }
        }
        PostgreSQLProxyConfiguration configuration = PostgreSQLProxyConfigurationBuilder.builder()
                .withClientSideConfig(ClientSideConfig.getDefault())
                .withTableSettings(tableSettings)
                .build(dataSource);
        proxyManager = new PostgreSQLadvisoryLockBasedProxyManager(configuration);
    }

    @Override
    protected ProxyManager<Long> getProxyManager() {
        return proxyManager;
    }

    @Override
    protected Long generateRandomKey() {
        return ThreadLocalRandom.current().nextLong(1_000_000_000);
    }

    @AfterClass
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
