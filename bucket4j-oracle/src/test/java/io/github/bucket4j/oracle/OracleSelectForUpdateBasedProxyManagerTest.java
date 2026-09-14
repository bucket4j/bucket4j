package io.github.bucket4j.oracle;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class OracleSelectForUpdateBasedProxyManagerTest extends AbstractDistributedBucketTest {

    private static OracleContainer container;
    private static DataSource dataSource;

    @BeforeAll
    public static void initializeInstance() throws SQLException {
        container = startOracleXeContainer();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings = BucketTableSettings.getDefault();
        final String INIT_TABLE_SCRIPT = "CREATE TABLE {0} ( {1} NUMBER NOT NULL PRIMARY KEY, {2} RAW(255), expires_at NUMBER)";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT, tableSettings.getTableName(), tableSettings.getIdName(), tableSettings.getStateName());
                statement.execute(query);
            }
        }

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "OracleSelectForUpdateBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jOracle.selectForUpdateBasedBuilder(dataSource)
                    .table("bucket")
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
        // gvenzl/oracle-xe is amd64-only, so on Apple Silicon it runs under emulation
        // and is prone to startup timeouts. gvenzl/oracle-free is a compatible multi-arch
        // (amd64 + arm64) image maintained by the same author, so it runs natively on Macos.
        DockerImageName imageName = DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart")
                .asCompatibleSubstituteFor("gvenzl/oracle-xe");
        OracleContainer oracle = new OracleContainer(imageName)
                .withDatabaseName("testDB")
                .withUsername("testUser")
                .withPassword("testPassword");
        oracle.start();
        return oracle;
    }

}
