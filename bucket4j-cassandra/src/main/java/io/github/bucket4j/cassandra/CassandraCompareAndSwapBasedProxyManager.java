package io.github.bucket4j.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.ByteUtils;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.expiration.NoneExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Compare-and-swap-based proxy manager for Apache Cassandra that uses the Cassandra Java Driver 4.x.
 *
 * <p>
 * Atomicity of read-modify-write cycle is achieved by Lightweight Transactions. The condition of conditional write
 * is a {@code bigint} version column instead of the state blob itself, in order to keep the amount of data
 * evaluated by Paxos as small as possible.
 *
 * <p>
 * Be aware that Lightweight Transactions are expensive, each successful write requires four round-trips between
 * Paxos participants. This integration is a reasonable choice for teams that already run Cassandra and do not want
 * to introduce another storage just for rate limiting, but it is not a replacement for in-memory grids or Redis
 * when the same bucket key is updated with high rate.
 *
 * <p>
 * Both deployment modes are supported:
 * <ul>
 *   <li>Dedicated session - the session is created with {@code CqlSession.builder().withKeyspace(...)},
 *   the keyspace is not specified on the builder and all statements use bare table name.</li>
 *   <li>Shared session - the default keyspace of the session belongs to the application, the keyspace is specified
 *   on the builder and all statements refer the table as {@code keyspace.table}.</li>
 * </ul>
 *
 * @param <K> type of primary key
 */
public class CassandraCompareAndSwapBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private static final long ABSENT_VERSION = 0L;
    private static final int MIN_TIME_TO_LIVE_SECONDS = 1;
    private static final int MAX_TIME_TO_LIVE_SECONDS = 630_720_000;

    private static final String KEY_MARKER = "bucket4j_key";
    private static final String STATE_MARKER = "bucket4j_state";
    private static final String NEW_VERSION_MARKER = "bucket4j_new_version";
    private static final String CURRENT_VERSION_MARKER = "bucket4j_current_version";
    private static final String TIME_TO_LIVE_MARKER = "bucket4j_time_to_live";

    private final CqlSession session;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final ConsistencyLevel readConsistencyLevel;
    private final ConsistencyLevel serialConsistencyLevel;
    private final String stateColumn;
    private final String versionColumn;
    private final PreparedStatement selectStatement;
    private final PreparedStatement insertStatement;
    private final PreparedStatement insertWithTimeToLiveStatement;
    private final PreparedStatement updateStatement;
    private final PreparedStatement updateWithTimeToLiveStatement;
    private final PreparedStatement deleteStatement;

    protected CassandraCompareAndSwapBasedProxyManager(Bucket4jCassandra.CassandraCompareAndSwapBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.session = builder.getSession();
        this.keyMapper = builder.getKeyMapper();
        this.expirationStrategy = builder.getClientSideConfig()
            .getExpirationAfterWriteStrategy()
            .orElseGet(ExpirationAfterWriteStrategy::none);
        this.readConsistencyLevel = builder.getReadConsistencyLevel();
        this.serialConsistencyLevel = builder.getSerialConsistencyLevel();
        this.stateColumn = builder.getStateColumn();
        this.versionColumn = builder.getVersionColumn();

        String table = resolveTable(builder);
        String keyColumn = builder.getKeyColumn();

        this.selectStatement = session.prepare(
            "SELECT %s, %s FROM %s WHERE %s = :%s"
                .formatted(stateColumn, versionColumn, table, keyColumn, KEY_MARKER));
        this.insertStatement = session.prepare(
            "INSERT INTO %s (%s, %s, %s) VALUES (:%s, :%s, :%s) IF NOT EXISTS"
                .formatted(table, keyColumn, stateColumn, versionColumn, KEY_MARKER, STATE_MARKER, NEW_VERSION_MARKER));
        this.insertWithTimeToLiveStatement = session.prepare(
            "INSERT INTO %s (%s, %s, %s) VALUES (:%s, :%s, :%s) IF NOT EXISTS USING TTL :%s"
                .formatted(table, keyColumn, stateColumn, versionColumn, KEY_MARKER, STATE_MARKER, NEW_VERSION_MARKER, TIME_TO_LIVE_MARKER));
        this.updateStatement = session.prepare(
            "UPDATE %s SET %s = :%s, %s = :%s WHERE %s = :%s IF %s = :%s"
                .formatted(table, stateColumn, STATE_MARKER, versionColumn, NEW_VERSION_MARKER, keyColumn, KEY_MARKER, versionColumn, CURRENT_VERSION_MARKER));
        this.updateWithTimeToLiveStatement = session.prepare(
            "UPDATE %s USING TTL :%s SET %s = :%s, %s = :%s WHERE %s = :%s IF %s = :%s"
                .formatted(table, TIME_TO_LIVE_MARKER, stateColumn, STATE_MARKER, versionColumn, NEW_VERSION_MARKER, keyColumn, KEY_MARKER, versionColumn, CURRENT_VERSION_MARKER));
        this.deleteStatement = session.prepare(
            "DELETE FROM %s WHERE %s = :%s"
                .formatted(table, keyColumn, KEY_MARKER));
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            private final String bucketKey = keyMapper.toString(key);
            private long currentVersion = ABSENT_VERSION;

            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                Row row = session.execute(buildSelectStatement(bucketKey, timeoutNanos)).one();
                currentVersion = readVersion(row);
                return readState(row);
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                long newVersion = currentVersion + 1;
                BoundStatement statement = buildCompareAndSwapStatement(bucketKey, newData, newState, currentVersion, newVersion, timeoutNanos);
                boolean applied = session.execute(statement).wasApplied();
                if (applied) {
                    currentVersion = newVersion;
                }
                return applied;
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        return new AsyncCompareAndSwapOperation() {
            private final String bucketKey = keyMapper.toString(key);
            private volatile long currentVersion = ABSENT_VERSION;

            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                return session.executeAsync(buildSelectStatement(bucketKey, timeoutNanos))
                    .toCompletableFuture()
                    .thenApply(resultSet -> {
                        Row row = resultSet.one();
                        currentVersion = readVersion(row);
                        return readState(row);
                    });
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState, Optional<Long> timeoutNanos) {
                long newVersion = currentVersion + 1;
                BoundStatement statement = buildCompareAndSwapStatement(bucketKey, newData, newState, currentVersion, newVersion, timeoutNanos);
                return session.executeAsync(statement)
                    .toCompletableFuture()
                    .thenApply(resultSet -> {
                        boolean applied = resultSet.wasApplied();
                        if (applied) {
                            currentVersion = newVersion;
                        }
                        return applied;
                    });
            }
        };
    }

    @Override
    public void removeProxy(K key) {
        session.execute(buildDeleteStatement(key));
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return session.executeAsync(buildDeleteStatement(key))
            .toCompletableFuture()
            .thenAccept(resultSet -> { });
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    private static String resolveTable(Bucket4jCassandra.CassandraCompareAndSwapBasedProxyManagerBuilder<?> builder) {
        String keyspace = builder.getKeyspace();
        return keyspace == null || keyspace.isBlank() ? builder.getTableName() : keyspace + "." + builder.getTableName();
    }

    private BoundStatement buildSelectStatement(String bucketKey, Optional<Long> timeoutNanos) {
        BoundStatement statement = selectStatement.bind()
            .setString(KEY_MARKER, bucketKey)
            .setConsistencyLevel(readConsistencyLevel)
            .setIdempotent(true);
        return applyTimeout(statement, timeoutNanos);
    }

    private BoundStatement buildDeleteStatement(K key) {
        return deleteStatement.bind()
            .setString(KEY_MARKER, keyMapper.toString(key))
            .setIdempotent(true);
    }

    private BoundStatement buildCompareAndSwapStatement(String bucketKey, byte[] newData, RemoteBucketState newState,
                                                        long currentVersion, long newVersion, Optional<Long> timeoutNanos) {
        boolean withTimeToLive = isTimeToLiveEnabled();
        BoundStatement statement;
        if (currentVersion == ABSENT_VERSION) {
            statement = withTimeToLive ? insertWithTimeToLiveStatement.bind() : insertStatement.bind();
        } else {
            statement = withTimeToLive ? updateWithTimeToLiveStatement.bind() : updateStatement.bind();
            statement = statement.setLong(CURRENT_VERSION_MARKER, currentVersion);
        }
        if (withTimeToLive) {
            statement = statement.setInt(TIME_TO_LIVE_MARKER, calculateTimeToLiveSeconds(newState));
        }
        statement = statement
            .setString(KEY_MARKER, bucketKey)
            .setByteBuffer(STATE_MARKER, ByteBuffer.wrap(newData))
            .setLong(NEW_VERSION_MARKER, newVersion)
            .setSerialConsistencyLevel(serialConsistencyLevel)
            .setIdempotent(false);
        return applyTimeout(statement, timeoutNanos);
    }

    private static BoundStatement applyTimeout(BoundStatement statement, Optional<Long> timeoutNanos) {
        return timeoutNanos.isPresent() ? statement.setTimeout(Duration.ofNanos(timeoutNanos.get())) : statement;
    }

    private long readVersion(Row row) {
        return row == null || row.isNull(versionColumn) ? ABSENT_VERSION : row.getLong(versionColumn);
    }

    private Optional<byte[]> readState(Row row) {
        return row == null ? Optional.empty() : Optional.ofNullable(toBytes(row.getByteBuffer(stateColumn)));
    }

    private boolean isTimeToLiveEnabled() {
        return expirationStrategy.getClass() != NoneExpirationAfterWriteStrategy.class;
    }

    private int calculateTimeToLiveSeconds(RemoteBucketState newState) {
        long timeToLiveMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
        if (timeToLiveMillis <= MIN_TIME_TO_LIVE_SECONDS * 1_000L) {
            return MIN_TIME_TO_LIVE_SECONDS;
        }
        long timeToLiveSeconds = timeToLiveMillis / 1_000L + (timeToLiveMillis % 1_000L == 0 ? 0 : 1);
        return (int) Math.min(timeToLiveSeconds, MAX_TIME_TO_LIVE_SECONDS);
    }

    private static byte[] toBytes(ByteBuffer buffer) {
        return buffer == null ? null : ByteUtils.getArray(buffer);
    }

}