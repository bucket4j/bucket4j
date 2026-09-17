package io.github.bucket4j.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.expiration.NoneExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import com.datastax.oss.driver.api.core.data.ByteUtils;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Compare-and-swap-based proxy manager for Apache Cassandra using the DataStax Java Driver 4.x.
 *
 * <p>Uses Cassandra Lightweight Transactions (LWT) for atomic read-modify-write semantics.
 * The LWT guard is a {@code bigint} version column — not the full state blob — to minimize
 * Paxos condition-evaluation overhead.
 *
 * <h3>Deployment modes</h3>
 * <ul>
 *   <li><b>Cluster-level / dedicated session</b>: create the {@link CqlSession} with
 *       {@code .withKeyspace("rate_limits")} so the session already targets the rate-limiting
 *       keyspace. Leave {@code .keyspace(...)} unset on the builder. All CQL statements use
 *       the bare table name.</li>
 *   <li><b>Microservice / shared session</b>: the session's default keyspace belongs to the
 *       core application. Set {@code .keyspace("rate_limits")} on the builder so that every
 *       CQL statement qualifies the table as {@code keyspace.table} and cannot accidentally
 *       touch the application keyspace.</li>
 * </ul>
 *
 * @param <K> type of primary key
 */
public class CassandraCompareAndSwapBasedProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final CqlSession session;
    private final Mapper<K> keyMapper;
    private final ExpirationAfterWriteStrategy expirationStrategy;
    private final ConsistencyLevel serialCL;
    private final String stateCol;
    private final String versionCol;
    private final PreparedStatement selectStmt;
    private final PreparedStatement insertStmt;
    private final PreparedStatement insertTtlStmt;
    private final PreparedStatement updateStmt;
    private final PreparedStatement updateTtlStmt;
    private final PreparedStatement deleteStmt;

    protected CassandraCompareAndSwapBasedProxyManager(Bucket4jCassandra.CassandraCompareAndSwapBasedProxyManagerBuilder<K> builder) {
        super(builder.getClientSideConfig());
        this.session           = builder.getSession();
        this.keyMapper         = builder.getKeyMapper();
        this.expirationStrategy = builder.getClientSideConfig()
                .getExpirationAfterWriteStrategy()
                .orElseGet(ExpirationAfterWriteStrategy::none);
        this.serialCL   = builder.getSerialConsistencyLevel();
        this.stateCol   = builder.getStateColumn();
        this.versionCol = builder.getVersionColumn();

        String tableRef = resolveTableRef(builder);
        String keyCol   = builder.getKeyColumn();

        this.selectStmt    = prepareSelect(tableRef, keyCol, stateCol, versionCol);
        this.insertStmt    = prepareInsert(tableRef, keyCol, stateCol, versionCol);
        this.insertTtlStmt = prepareInsertTtl(tableRef, keyCol, stateCol, versionCol);
        this.updateStmt    = prepareUpdate(tableRef, keyCol, stateCol, versionCol);
        this.updateTtlStmt = prepareUpdateTtl(tableRef, keyCol, stateCol, versionCol);
        this.deleteStmt    = prepareDelete(tableRef, keyCol);
    }

    /**
     * Returns the fully-qualified table reference ({@code keyspace.table}) when a keyspace is
     * configured on the builder, or the bare table name when it is not.
     *
     * <p><b>Shared session (microservice):</b> the builder's {@code keyspace} is set, so every
     * prepared statement explicitly targets the rate-limiting keyspace regardless of the session's
     * own default keyspace.
     *
     * <p><b>Dedicated session (cluster):</b> no keyspace override; the session was created
     * with {@code CqlSession.builder().withKeyspace("rate_limits")}, so the bare table name is
     * sufficient and Cassandra resolves it against the session's default keyspace.
     */
    private static String resolveTableRef(Bucket4jCassandra.CassandraCompareAndSwapBasedProxyManagerBuilder<?> builder) {
        String ks = builder.getKeyspace();
        return (ks != null && !ks.isBlank()) ? ks + "." + builder.getTableName() : builder.getTableName();
    }

    private PreparedStatement prepareSelect(String tableRef, String keyCol, String stateCol, String versionCol) {
        return session.prepare(
                "SELECT %s, %s FROM %s WHERE %s = :key".formatted(stateCol, versionCol, tableRef, keyCol)
        );
    }

    private PreparedStatement prepareInsert(String tableRef, String keyCol, String stateCol, String versionCol) {
        return session.prepare(
                "INSERT INTO %s (%s, %s, %s) VALUES (:key, :state, :nextVersion) IF NOT EXISTS".formatted(tableRef, keyCol, stateCol, versionCol)
        );
    }

    private PreparedStatement prepareInsertTtl(String tableRef, String keyCol, String stateCol, String versionCol) {
        return session.prepare(
                "INSERT INTO %s (%s, %s, %s) VALUES (:key, :state, :nextVersion) IF NOT EXISTS USING TTL :ttl".formatted(tableRef, keyCol, stateCol, versionCol)
        );
    }

    private PreparedStatement prepareUpdate(String tableRef, String keyCol, String stateCol, String versionCol) {
        // Guards on version (bigint) — not the state blob — to minimize Paxos condition size.
        return session.prepare(
                "UPDATE %s SET %s = :state, %s = :nextVersion WHERE %s = :key IF %s = :currentVersion".formatted(tableRef, stateCol, versionCol, keyCol, versionCol)
        );
    }

    private PreparedStatement prepareUpdateTtl(String tableRef, String keyCol, String stateCol, String versionCol) {
        return session.prepare(
                "UPDATE %s USING TTL :ttl SET %s = :state, %s = :nextVersion WHERE %s = :key IF %s = :currentVersion".formatted(tableRef, stateCol, versionCol, keyCol, versionCol)
        );
    }

    private PreparedStatement prepareDelete(String tableRef, String keyCol) {
        return session.prepare(
                "DELETE FROM %s WHERE %s = :key".formatted(tableRef, keyCol)
        );
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            private final String k = keyMapper.toString(key);
            private long currentVersion;

            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeoutNanos) {
                BoundStatement stmt = applyTimeout(selectStmt.bind().setString("key", k), timeoutNanos);
                Row row = session.execute(stmt).one();
                if (row == null) {
                    currentVersion = 0L;
                    return Optional.empty();
                }
                currentVersion = row.isNull(versionCol) ? 0L : row.getLong(versionCol);
                return Optional.ofNullable(ByteUtils.getArray(row.getByteBuffer(stateCol)));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState,
                                          Optional<Long> timeoutNanos) {
                // LWT statements are non-idempotent — the driver will NOT auto-retry on
                // WriteTimeoutException. Any retry goes through the full CAS loop, which
                // re-reads state via getStateData() before attempting another write.
                long nextVersion = currentVersion + 1;
                BoundStatement stmt = buildCasStmt(k, originalData, newData, newState,
                        currentVersion, nextVersion, timeoutNanos);
                boolean applied = session.execute(stmt).wasApplied();
                if (applied) {
                    currentVersion = nextVersion;
                }
                return applied;
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        return new AsyncCompareAndSwapOperation() {
            private final String k = keyMapper.toString(key);
            private volatile long currentVersion;

            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeoutNanos) {
                BoundStatement stmt = applyTimeout(selectStmt.bind().setString("key", k), timeoutNanos);
                return session.executeAsync(stmt).toCompletableFuture()
                        .thenApply(rs -> {
                            Row row = rs.one();
                            if (row == null) {
                                currentVersion = 0L;
                                return Optional.empty();
                            }
                            currentVersion = row.isNull(versionCol) ? 0L : row.getLong(versionCol);
                            return Optional.<byte[]>ofNullable(toBytes(row.getByteBuffer(stateCol)));
                        });
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData,
                                                              RemoteBucketState newState,
                                                              Optional<Long> timeoutNanos) {
                long nextVersion = currentVersion + 1;
                BoundStatement stmt = buildCasStmt(k, originalData, newData, newState,
                        currentVersion, nextVersion, timeoutNanos);
                return session.executeAsync(stmt).toCompletableFuture()
                        .thenApply(rs -> {
                            boolean applied = rs.wasApplied();
                            if (applied) {
                                currentVersion = nextVersion;
                            }
                            return applied;
                        });
            }
        };
    }

    @Override
    public void removeProxy(K key) {
        session.execute(deleteStmt.bind().setString("key", keyMapper.toString(key)));
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return session.executeAsync(deleteStmt.bind().setString("key", keyMapper.toString(key)))
                .toCompletableFuture()
                .thenApply(ignored -> null);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

    /**
     * Builds the LWT bound statement for a CAS attempt.
     *
     * <ul>
     *   <li>INSERT path ({@code originalData == null}): row does not yet exist.</li>
     *   <li>UPDATE path: row exists; guard on {@code IF version = currentVersion}.</li>
     * </ul>
     *
     * Serial consistency ({@link #serialCL}) is applied only to these LWT statements — not to
     * the plain SELECT in {@code getStateData}.
     */
    private BoundStatement buildCasStmt(String k, byte[] originalData, byte[] newData,
                                         RemoteBucketState newState, long currentVersion,
                                         long nextVersion, Optional<Long> timeoutNanos) {
        boolean useTtl = hasTtl();
        BoundStatement stmt;
        if (originalData == null) {
            if (useTtl) {
                stmt = insertTtlStmt.bind()
                        .setString("key", k)
                        .setByteBuffer("state", toBuffer(newData))
                        .setLong("nextVersion", nextVersion)
                        .setInt("ttl", ttlSeconds(newState));
            } else {
                stmt = insertStmt.bind()
                        .setString("key", k)
                        .setByteBuffer("state", toBuffer(newData))
                        .setLong("nextVersion", nextVersion);
            }
        } else {
            if (useTtl) {
                stmt = updateTtlStmt.bind()
                        .setInt("ttl", ttlSeconds(newState))
                        .setByteBuffer("state", toBuffer(newData))
                        .setLong("nextVersion", nextVersion)
                        .setString("key", k)
                        .setLong("currentVersion", currentVersion);
            } else {
                stmt = updateStmt.bind()
                        .setByteBuffer("state", toBuffer(newData))
                        .setLong("nextVersion", nextVersion)
                        .setString("key", k)
                        .setLong("currentVersion", currentVersion);
            }
        }
        return applySerial(applyTimeout(stmt, timeoutNanos));
    }

    private BoundStatement applyTimeout(BoundStatement stmt, Optional<Long> timeoutNanos) {
        return timeoutNanos.isPresent()
                ? stmt.setTimeout(Duration.ofNanos(timeoutNanos.get()))
                : stmt;
    }

    private BoundStatement applySerial(BoundStatement stmt) {
        return stmt.setSerialConsistencyLevel(serialCL);
    }

    private boolean hasTtl() {
        return expirationStrategy.getClass() != NoneExpirationAfterWriteStrategy.class;
    }

    private int ttlSeconds(RemoteBucketState newState) {
        long ttlMillis = expirationStrategy.calculateTimeToLiveMillis(newState, currentTimeNanos());
        return (int) Math.max(1, ttlMillis / 1_000);
    }

    private static ByteBuffer toBuffer(byte[] bytes) {
        return bytes == null ? null : ByteBuffer.wrap(bytes);
    }

}
