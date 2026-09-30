package io.github.bucket4j.db2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.bucket4j.db2.util.HexUtil;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.BackwardCompatibilityStateCheckHelper;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.db2.Db2Container;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

        BackwardCompatibilityStateCheckHelper<Long> backwardCompatibilityHelper = new JdbcBackwardCompatibilityStateCheckHelper("bucket", "id", "state");
        BackwardCompatibilityStateCheckHelper<Long> backwardCompatibilityHelperStringKeyTable = new JdbcBackwardCompatibilityStateCheckHelper("buckets_String_key", "id", "state");

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "Db2SelectForUpdateBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jDb2.selectForUpdateBasedBuilder(dataSource)
                    .table("bucket")
                    .idColumn("id")
                    .stateColumn("state")
            ).checkExpiration().checkStateBackwardCompatibility(backwardCompatibilityHelper),
            new ProxyManagerSpec<>(
                "Db2SelectForUpdateBasedProxyManager_StringKey",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jDb2.selectForUpdateBasedBuilder(dataSource)
                    .table("buckets_String_key")
                    .idColumn("id")
                    .stateColumn("state")
            ).checkExpiration().checkStateBackwardCompatibility(backwardCompatibilityHelperStringKeyTable)
        );
    }

    private static class JdbcBackwardCompatibilityStateCheckHelper implements BackwardCompatibilityStateCheckHelper<Long> {

        private final String table;
        private final String idColumn;
        private final String stateColumn;

        private JdbcBackwardCompatibilityStateCheckHelper(String table, String idColumn, String stateColumn) {
            this.table = table;
            this.idColumn = idColumn;
            this.stateColumn = stateColumn;
        }

        @Override
        public byte[] getRawState(Long key) {
            // Db2SelectForUpdateBasedProxyManager stores the state as a hex-encoded string
            // in a VARCHAR column, see Db2SelectForUpdateBasedProxyManager#tryLockAndGet
            String query = "SELECT " + stateColumn + " FROM " + table + " WHERE " + idColumn + " = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setLong(1, key);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("There is no row for key " + key + " in table " + table);
                    }
                    String hex = resultSet.getString(1);
                    return hex == null ? null : HexUtil.hexToBinary(hex);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setRawState(Long key, byte[] state) {
            String query = "UPDATE " + table + " SET " + stateColumn + " = ? WHERE " + idColumn + " = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, HexUtil.hexFromBinary(state));
                statement.setLong(2, key);
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

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
        DockerImageName imageName = DockerImageName.parse("ibmcom/db2:11.5.0.0a")
                .asCompatibleSubstituteFor("icr.io/db2_community/db2");
        Db2Container container = new Db2Container(imageName).acceptLicense();
        container.start();
        return container;
    }
}
