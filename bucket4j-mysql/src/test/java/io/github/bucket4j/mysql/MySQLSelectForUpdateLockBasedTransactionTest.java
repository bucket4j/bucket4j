package io.github.bucket4j.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.jdbc.BucketTableSettings;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void shouldNotDeadlockWhenInitializingDistinctBucketsConcurrently() throws Exception {
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, readTransactionIsolation());

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(1).refillIntervally(1, Duration.ofMinutes(1)))
                .build();
        ProxyManager<Long> proxyManager = Bucket4jMySQL.selectForUpdateBasedBuilder(dataSource).build();

        long deadlocksBefore = readDeadlockCount();
        initializeDistinctBucketsConcurrently(proxyManager, configuration);
        long deadlocksAfter = readDeadlockCount();

        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, readTransactionIsolation());
        assertEquals(
                deadlocksBefore,
                deadlocksAfter,
                "Concurrent initialization of distinct buckets must not cause MySQL deadlocks"
        );
    }

    private static void initializeDistinctBucketsConcurrently(
            ProxyManager<Long> proxyManager,
            BucketConfiguration configuration
    ) throws Exception {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(threadCount);
        List<Future<?>> initializations = new ArrayList<>(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                long bucketKey = Long.MIN_VALUE + i;
                initializations.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        proxyManager.builder().build(bucketKey, () -> configuration).tryConsume(1);
                    } finally {
                        completed.countDown();
                    }
                    return null;
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS), "Workers did not become ready in time");
            start.countDown();
            assertTrue(
                    completed.await(30, TimeUnit.SECONDS),
                    "Concurrent bucket initialization did not finish in time"
            );
            for (Future<?> initialization : initializations) {
                initialization.get();
            }
        } finally {
            executor.shutdownNow();
        }
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

    private static int readTransactionIsolation() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getTransactionIsolation();
        }
    }

    private static long readDeadlockCount() throws SQLException {
        // The application test user does not have permission to read performance_schema.
        try (Connection connection = DriverManager.getConnection(
                container.getJdbcUrl(),
                "root",
                container.getPassword()
        );
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT SUM_ERROR_RAISED FROM performance_schema.events_errors_summary_global_by_error " +
                             "WHERE ERROR_NUMBER = 1213"
             )) {
            assertTrue(resultSet.next(), "MySQL did not expose the deadlock error summary");
            return resultSet.getLong("SUM_ERROR_RAISED");
        }
    }

    private static MySQLContainer startMySQLContainer() {
        MySQLContainer container = new MySQLContainer(DockerImageName.parse("mysql:8.0.36"));
        container.start();
        return container;
    }

}
