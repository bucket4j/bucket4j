package io.github.bucket4j.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfigurationBuilder;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

public class MySQLSelectForUpdateLockBasedTransactionTest extends AbstractDistributedBucketTest<Long> {

    private static MySQLContainer container;
    private static DataSource dataSource;
    private static MySQLSelectForUpdateBasedProxyManager<Long> proxyManager;

    @BeforeAll
    public static void initializeInstance() throws SQLException {
        container = startMySQLContainer();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings = BucketTableSettings.getDefault();
        final String INIT_TABLE_SCRIPT = "CREATE TABLE IF NOT EXISTS {0}({1} BIGINT PRIMARY KEY, {2} BLOB)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT, tableSettings.getTableName(), tableSettings.getIdName(), tableSettings.getStateName());
                statement.execute(query);
            }
        }
        SQLProxyConfiguration<Long> configuration = SQLProxyConfigurationBuilder.builder()
                .withClientSideConfig(ClientSideConfig.getDefault())
                .withTableSettings(tableSettings)
                .build(dataSource);
        proxyManager = new MySQLSelectForUpdateBasedProxyManager<>(configuration);
    }

    @Override
    protected ProxyManager<Long> getProxyManager() {
        return proxyManager;
    }

    @Override
    protected Long generateRandomKey() {
        return ThreadLocalRandom.current().nextLong(1_000_000_000);
    }

    @AfterAll
    public static void shutdown() {
        if (container != null) {
            container.stop();
        }
    }

    private static DataSource createJdbcDataSource(MySQLContainer container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setMaximumPoolSize(100);
        return new HikariDataSource(hikariConfig);
    }

    private static MySQLContainer startMySQLContainer() {
        MySQLContainer container = new MySQLContainer();
        container.start();
        return container;
    }

}
