import com.datastax.oss.driver.api.core.CqlSession;
import io.github.bucket4j.cassandra.Bucket4jCassandra;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.tck.AbstractDistributedBucketTest;
import io.github.bucket4j.tck.ProxyManagerSpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.cassandra.CassandraContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CassandraTest extends AbstractDistributedBucketTest {

    private static final String KEYSPACE = "bucket4j_test";
    private static final String TABLE = "buckets";
    private static final String KEY_COLUMN = "bucket_key";
    private static final String STATE_COLUMN = "bucket_state";
    private static final String VERSION_COLUMN = "bucket_version";

    private static CassandraContainer container;
    private static CqlSession sessionWithoutDefaultKeyspace;
    private static CqlSession sessionWithDefaultKeyspace;

    @BeforeAll
    public static void setupCassandra() {
        container = new CassandraContainer("cassandra:5.0");
        container.start();

        sessionWithoutDefaultKeyspace = CqlSession.builder()
            .addContactPoint(container.getContactPoint())
            .withLocalDatacenter(container.getLocalDatacenter())
            .build();

        sessionWithoutDefaultKeyspace.execute(
            "CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE +
                " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
        sessionWithoutDefaultKeyspace.execute(
            "CREATE TABLE IF NOT EXISTS " + KEYSPACE + "." + TABLE + " (" +
                KEY_COLUMN + " text PRIMARY KEY, " +
                STATE_COLUMN + " blob, " +
                VERSION_COLUMN + " bigint)");

        sessionWithDefaultKeyspace = CqlSession.builder()
            .addContactPoint(container.getContactPoint())
            .withLocalDatacenter(container.getLocalDatacenter())
            .withKeyspace(KEYSPACE)
            .build();

        specs = List.of(
            new ProxyManagerSpec<>(
                "CassandraCompareAndSwapBasedProxyManager_DedicatedSession",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jCassandra.compareAndSwapBasedBuilder(sessionWithDefaultKeyspace)
                    .tableName(TABLE)
                    .keyColumn(KEY_COLUMN)
                    .stateColumn(STATE_COLUMN)
                    .versionColumn(VERSION_COLUMN)
            ).checkExpiration(),
            new ProxyManagerSpec<>(
                "CassandraCompareAndSwapBasedProxyManager_SharedSession",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jCassandra.compareAndSwapBasedBuilder(sessionWithoutDefaultKeyspace)
                    .keyspace(KEYSPACE)
                    .tableName(TABLE)
                    .keyColumn(KEY_COLUMN)
                    .stateColumn(STATE_COLUMN)
                    .versionColumn(VERSION_COLUMN)
            ).checkExpiration(),
            new ProxyManagerSpec<>(
                "CassandraCompareAndSwapBasedProxyManager_DedicatedSessionWithLongKeys",
                () -> ThreadLocalRandom.current().nextLong(),
                () -> Bucket4jCassandra.compareAndSwapBasedBuilder(sessionWithDefaultKeyspace, Mapper.LONG)
                    .tableName(TABLE)
                    .keyColumn(KEY_COLUMN)
                    .stateColumn(STATE_COLUMN)
                    .versionColumn(VERSION_COLUMN)
            ).checkExpiration(),
            new ProxyManagerSpec<>(
                "CassandraCompareAndSwapBasedProxyManager_SharedSessionWithLongKeys",
                () -> ThreadLocalRandom.current().nextLong(),
                () -> Bucket4jCassandra.compareAndSwapBasedBuilder(sessionWithoutDefaultKeyspace, Mapper.LONG)
                    .keyspace(KEYSPACE)
                    .tableName(TABLE)
                    .keyColumn(KEY_COLUMN)
                    .stateColumn(STATE_COLUMN)
                    .versionColumn(VERSION_COLUMN)
            ).checkExpiration()
        );
    }

    @AfterAll
    public static void cleanupCassandra() {
        if (sessionWithDefaultKeyspace != null) {
            sessionWithDefaultKeyspace.close();
        }
        if (sessionWithoutDefaultKeyspace != null) {
            sessionWithoutDefaultKeyspace.close();
        }
        if (container != null) {
            container.stop();
        }
    }
}