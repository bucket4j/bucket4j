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

    private static final String KEYSPACE   = "bucket4j_test";
    private static final String TABLE      = "rate_limits";
    private static final String KEY_COL    = "key";
    private static final String STATE_COL  = "state";
    private static final String VERSION_COL = "version";

    private static CassandraContainer<?> container;

    /** Session without a default keyspace — simulates a shared/microservice session. */
    private static CqlSession sharedSession;

    /** Session with {@value KEYSPACE} as its default keyspace — simulates a dedicated cluster session. */
    private static CqlSession dedicatedSession;

    @BeforeAll
    public static void setup() {
        container = new CassandraContainer<>("cassandra:5.0");
        container.start();

        // Bootstrap: shared session has no default keyspace; used for schema creation and microservice specs.
        sharedSession = CqlSession.builder()
                .addContactPoint(container.getContactPoint())
                .withLocalDatacenter(container.getLocalDatacenter())
                .build();

        sharedSession.execute(
                "CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE +
                " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}"
        );
        sharedSession.execute(
                "CREATE TABLE IF NOT EXISTS " + KEYSPACE + "." + TABLE + " (" +
                KEY_COL + "     text    PRIMARY KEY, " +
                STATE_COL + "   blob, " +
                VERSION_COL + " bigint" +
                ")"
        );

        dedicatedSession = CqlSession.builder()
                .addContactPoint(container.getContactPoint())
                .withLocalDatacenter(container.getLocalDatacenter())
                .withKeyspace(KEYSPACE)
                .build();

        specs = List.of(
            new ProxyManagerSpec<>(
                "CassandraCompareAndSwap_DedicatedSession",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jCassandra.compareAndSwapBasedBuilder(dedicatedSession)
                        .tableName(TABLE)
                        .keyColumn(KEY_COL)
                        .stateColumn(STATE_COL)
                        .versionColumn(VERSION_COL)
            ).checkExpiration(),

            new ProxyManagerSpec<>(
                "CassandraCompareAndSwap_SharedSession",
                () -> UUID.randomUUID().toString(),
                () -> Bucket4jCassandra.compareAndSwapBasedBuilder(sharedSession)
                        .keyspace(KEYSPACE)
                        .tableName(TABLE)
                        .keyColumn(KEY_COL)
                        .stateColumn(STATE_COL)
                        .versionColumn(VERSION_COL)
            ).checkExpiration(),

            new ProxyManagerSpec<>(
                "CassandraCompareAndSwap_DedicatedSession_LongKeys",
                () -> ThreadLocalRandom.current().nextLong(),
                () -> Bucket4jCassandra.compareAndSwapBasedBuilder(dedicatedSession, Mapper.LONG)
                        .tableName(TABLE)
                        .keyColumn(KEY_COL)
                        .stateColumn(STATE_COL)
                        .versionColumn(VERSION_COL)
            ).checkExpiration(),

            new ProxyManagerSpec<>(
                "CassandraCompareAndSwap_SharedSession_LongKeys",
                () -> ThreadLocalRandom.current().nextLong(),
                () -> Bucket4jCassandra.compareAndSwapBasedBuilder(sharedSession, Mapper.LONG)
                        .keyspace(KEYSPACE)
                        .tableName(TABLE)
                        .keyColumn(KEY_COL)
                        .stateColumn(STATE_COL)
                        .versionColumn(VERSION_COL)
            ).checkExpiration()
        );
    }

    @AfterAll
    public static void teardown() {
        if (dedicatedSession != null) {
            dedicatedSession.close();
        }
        if (sharedSession != null) {
            sharedSession.close();
        }
        if (container != null) {
            container.stop();
        }
    }
}
