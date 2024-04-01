package io.github.bucket4j.mssql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.jdbc.SQLProxyConfiguration;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MSSQLServerContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class MSSQLSelectForUpdateBasedProxyManagerTest extends AbstractDistributedBucketTest {

    private static MSSQLServerContainer container;
    private static HikariDataSource dataSource;

    @BeforeAll
    public static void initializeInstance() throws SQLException {
        container = startMsSqlContainer();
        dataSource = createJdbcDataSource(container);
        BucketTableSettings tableSettings = BucketTableSettings.getDefault();
        final String INIT_TABLE_SCRIPT = "CREATE TABLE {0} ( {1} INT NOT NULL PRIMARY KEY, {2} BINARY(256) )";
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = MessageFormat.format(INIT_TABLE_SCRIPT, tableSettings.getTableName(), tableSettings.getIdName(), tableSettings.getStateName());
                statement.execute(query);
            }
        }

        specs = Arrays.asList(
            new ProxyManagerSpec<>(
                "MSSQLSelectForUpdateBasedProxyManager",
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000),
                () -> Bucket4jMSSQL.selectForUpdateBasedBuilder(dataSource)
                    .table("bucket")
                    .idColumn("id")
                    .stateColumn("state")
            )
        );
    }

    /**
     * This tests emulates transactional behavior of MSSQLSelectForUpdateBasedProxyManager
     */
    @Test
    public void transactionIsolationTest() throws SQLException, InterruptedException {
        int threadCount = 8;
        int opsCount = 1_000;

        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE table_with_counter (id INT NOT NULL PRIMARY KEY, some_counter INT)");
            }
        }

        CountDownLatch startLatch = new CountDownLatch(threadCount);
        CountDownLatch stopLatch = new CountDownLatch(threadCount);
        AtomicInteger opsCounter = new AtomicInteger(opsCount);
        ConcurrentHashMap<Integer, Integer> updatesByThread = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, Throwable> errors = new ConcurrentHashMap<>();
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    while (opsCounter.decrementAndGet() >= 0) {
                        Integer currenValue = null;
                        try (Connection connection = dataSource.getConnection()) {
                            connection.setAutoCommit(false);
                            try (Statement selectStatement = connection.createStatement()) {
                                try (ResultSet rs = selectStatement.executeQuery("SELECT some_counter FROM table_with_counter WITH(ROWLOCK, UPDLOCK) WHERE id=666")) {
                                    if (rs.next()) {
                                        currenValue = rs.getInt("some_counter");
                                        try (Statement updateStatement = connection.createStatement()) {
                                            updateStatement.executeUpdate("UPDATE table_with_counter SET some_counter = "+(currenValue + 1)+" WHERE id=666");
                                        }
                                        connection.commit();
                                        updatesByThread.compute(threadId, (key, current) -> current == null ? 1 : current + 1);
                                        connection.setAutoCommit(true);
                                    } else {
                                        connection.rollback();
                                    }
                                }
                            }
                        }
                        if (currenValue == null) {
                            opsCounter.incrementAndGet();
                            try (Connection connection = dataSource.getConnection()) {
                                connection.setAutoCommit(false);
                                try (Statement insertStatement = connection.createStatement()) {
                                    insertStatement.execute("INSERT INTO table_with_counter(id, some_counter) VALUES (666, 0)");
                                    connection.commit();
                                } catch (SQLException e) {
                                    connection.rollback();
                                    if (e.getErrorCode() == 2627 || e.getErrorCode() == 1205) {
                                        // do nothing
                                    } else {
                                        throw e;
                                    }
                                }
                                connection.setAutoCommit(true);
                            }
                        }
                    }
                } catch (Throwable e) {
                    errors.put(threadId, e);
                    e.printStackTrace();
                } finally {
                    stopLatch.countDown();
                }
            }, "Updater-thread-" + i).start();
        }
        stopLatch.await();

        int currenValue;
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT some_counter FROM table_with_counter WHERE id=666")) {
                    rs.next();
                    currenValue = rs.getInt("some_counter");
                }
            }
        }
        System.out.println("Counts " + currenValue);
        System.out.println("Failed threads " + errors.keySet());
        System.out.println("Updates by thread " + updatesByThread);
        assertTrue(errors.isEmpty());
        assertEquals(opsCount, currenValue);
    }

    @AfterAll
    public static void shutdown() {
        try {
            if (dataSource != null) {
                dataSource.shutdown();
            }
        } finally {
            if (container != null) {
                container.stop();
            }
        }
    }

    private static HikariDataSource createJdbcDataSource(MSSQLServerContainer container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setMaximumPoolSize(100);
        return new HikariDataSource(hikariConfig);
    }

    private static MSSQLServerContainer startMsSqlContainer() {
        MSSQLServerContainer mssqlServerContainer = new MSSQLServerContainer().acceptLicense();
        mssqlServerContainer.start();
        return mssqlServerContainer;
    }

}
