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
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(1).refillIntervally(1, Duration.ofMinutes(1)))
                .build();
        TransactionIsolationTrackingDataSource trackingDataSource =
                new TransactionIsolationTrackingDataSource(dataSource);
        ProxyManager<Long> proxyManager = Bucket4jMySQL.selectForUpdateBasedBuilder(trackingDataSource).build();

        long deadlocksBefore = readDeadlockCount();
        initializeDistinctBucketsConcurrently(proxyManager, configuration);
        long deadlocksAfter = readDeadlockCount();

        trackingDataSource.assertEveryConnectionRestoredBeforeClose();
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

    private static class TransactionIsolationTrackingDataSource implements DataSource {

        private final DataSource delegate;
        private final List<TrackedConnectionState> connectionStates = Collections.synchronizedList(new ArrayList<>());

        private TransactionIsolationTrackingDataSource(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return track(delegate.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return track(delegate.getConnection(username, password));
        }

        private Connection track(Connection connection) throws SQLException {
            TrackedConnectionState connectionState =
                    new TrackedConnectionState(connection.getTransactionIsolation());
            connectionStates.add(connectionState);
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class},
                    new TransactionIsolationTrackingConnectionHandler(connection, connectionState)
            );
        }

        private void assertEveryConnectionRestoredBeforeClose() {
            assertFalse(connectionStates.isEmpty(), "No manager-owned connections were tracked");
            List<String> violations = new ArrayList<>();
            boolean atLeastOneConnectionRequiredRestoration = false;
            synchronized (connectionStates) {
                for (int i = 0; i < connectionStates.size(); i++) {
                    TrackedConnectionState connectionState = connectionStates.get(i);
                    if (connectionState.originalTransactionIsolation != Connection.TRANSACTION_READ_COMMITTED) {
                        atLeastOneConnectionRequiredRestoration = true;
                        if (!connectionState.switchedToReadCommitted) {
                            violations.add("Connection #" + i + " was not switched to READ_COMMITTED");
                        }
                    }
                    if (!connectionState.closed) {
                        violations.add("Connection #" + i + " was not closed");
                    } else if (connectionState.closeCheckFailure != null) {
                        violations.add(
                                "Connection #" + i + " failed to read isolation before close: " +
                                        connectionState.closeCheckFailure
                        );
                    } else if (connectionState.isolationBeforeClose != connectionState.originalTransactionIsolation) {
                        violations.add(
                                "Connection #" + i + " was closed with isolation " +
                                        connectionState.isolationBeforeClose +
                                        " instead of original isolation " +
                                        connectionState.originalTransactionIsolation
                        );
                    }
                }
            }
            assertTrue(
                    atLeastOneConnectionRequiredRestoration,
                    "No manager-owned connection required transaction isolation restoration"
            );
            assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return delegate.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            delegate.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            delegate.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return delegate.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return iface.isInstance(this) || delegate.isWrapperFor(iface);
        }

    }

    private static class TransactionIsolationTrackingConnectionHandler implements InvocationHandler {

        private final Connection delegate;
        private final TrackedConnectionState connectionState;

        private TransactionIsolationTrackingConnectionHandler(
                Connection delegate,
                TrackedConnectionState connectionState
        ) {
            this.delegate = delegate;
            this.connectionState = connectionState;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            boolean switchesToReadCommitted = isSetTransactionIsolationToReadCommitted(method, args);
            if (isClose(method) && !connectionState.closed) {
                try {
                    connectionState.isolationBeforeClose = delegate.getTransactionIsolation();
                } catch (SQLException e) {
                    connectionState.closeCheckFailure = e;
                }
                connectionState.closed = true;
            }
            try {
                Object result = method.invoke(delegate, args);
                if (switchesToReadCommitted) {
                    connectionState.switchedToReadCommitted = true;
                }
                return result;
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private static boolean isClose(Method method) {
            return method.getName().equals("close") && method.getParameterCount() == 0;
        }

        private static boolean isSetTransactionIsolationToReadCommitted(Method method, Object[] args) {
            return method.getName().equals("setTransactionIsolation") &&
                    method.getParameterCount() == 1 &&
                    args != null &&
                    args.length == 1 &&
                    args[0] instanceof Integer &&
                    (Integer) args[0] == Connection.TRANSACTION_READ_COMMITTED;
        }

    }

    private static class TrackedConnectionState {

        private final int originalTransactionIsolation;
        private volatile boolean closed;
        private volatile boolean switchedToReadCommitted;
        private volatile int isolationBeforeClose;
        private volatile SQLException closeCheckFailure;

        private TrackedConnectionState(int originalTransactionIsolation) {
            this.originalTransactionIsolation = originalTransactionIsolation;
        }

    }

}
