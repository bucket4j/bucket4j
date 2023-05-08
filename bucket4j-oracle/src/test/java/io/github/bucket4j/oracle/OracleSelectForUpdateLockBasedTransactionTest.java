package io.github.bucket4j.oracle;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfigurationBuilder;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.containers.OracleContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

public class OracleSelectForUpdateLockBasedTransactionTest extends AbstractDistributedBucketTest<Long> {

    private static OracleContainer container;
    private static DataSource dataSource;
    private static OracleSelectForUpdateBasedProxyManager<Long> proxyManager;

    @BeforeClass
    public static void initializeInstance() throws SQLException {
        container = startOracleXeContainer();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings = BucketTableSettings.getDefault();
        final String INIT_TABLE_SCRIPT = "CREATE TABLE bucket ( id NUMBER NOT NULL PRIMARY KEY, state RAW(255) )";
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
        proxyManager = new OracleSelectForUpdateBasedProxyManager<>(configuration);
        Long key = 1L;
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
