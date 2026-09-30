package io.github.bucket4j.mariadb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.mariadb.MariaDBContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class MariaDBSelectForUpdateBasedTransactionTest extends AbstractDistributedBucketTest {

    private static MariaDBContainer container;
    private static DataSource dataSource;

    @BeforeAll
    public static void initializeInstance() throws SQLException {
        container = startMariaDbContainer();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings = BucketTableSettings.customSettings("test.bucket", "id", "state");
        final String INIT_TABLE_SCRIPT = "CREATE TABLE IF NOT EXISTS {0}({1} BIGINT PRIMARY KEY, {2} BLOB, expires_at BIGINT)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT, tableSettings.getTableName(), tableSettings.getIdName(), tableSettings.getStateName());
                statement.execute(query);
            }
        }

        BackwardCompatibilityStateCheckHelper<Long> backwardCompatibilityHelper = new BackwardCompatibilityStateCheckHelper<>() {
            @Override
            public byte[] getRawState(Long key) {
                String query = "SELECT state FROM test.bucket WHERE id = ?";
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setLong(1, key);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new IllegalStateException("There is no row for key " + key + " in table test.bucket");
                        }
                        return resultSet.getBytes(1);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void setRawState(Long key, byte[] state) {
                String query = "UPDATE test.bucket SET state = ? WHERE id = ?";
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

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "MariaDBSelectForUpdateBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jMariaDB.selectForUpdateBasedBuilder(dataSource)
                    .table("test.bucket")
                    .idColumn("id")
                    .stateColumn("state")
            ).checkExpiration().checkStateBackwardCompatibility(backwardCompatibilityHelper)
        );
    }

    @AfterAll
    public static void shutdown() {
        if (container != null) {
            container.stop();
        }
    }

    private static DataSource createJdbcDataSource(MariaDBContainer container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setMaximumPoolSize(100);
        return new HikariDataSource(hikariConfig);
    }

    private static MariaDBContainer startMariaDbContainer() {
        MariaDBContainer container = new MariaDBContainer("mariadb:11.2");
        container.start();
        return container;
    }

}
