package io.github.bucket4j.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;

import java.util.Objects;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

/**
 * Entry point for Apache Cassandra integration that uses the Cassandra Java Driver 4.x.
 *
 * <p>
 * Schema attributes({@code tableName}, {@code keyColumn}, {@code stateColumn}, {@code versionColumn}) have no defaults
 * and must be specified explicitly, so that builder maps exactly to already existing table.
 * The {@code keyspace} is optional, it is required only when session has no default keyspace,
 * or when another keyspace than the session default should be targeted.
 *
 * <pre>{@code
 * CREATE KEYSPACE rate_limits WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
 *
 * CREATE TABLE rate_limits.buckets (
 *     bucket_key     text PRIMARY KEY,
 *     bucket_state   blob,
 *     bucket_version bigint
 * );
 * }</pre>
 *
 * <pre>{@code
 * CassandraCompareAndSwapBasedProxyManager<String> proxyManager = Bucket4jCassandra
 *     .compareAndSwapBasedBuilder(session)
 *     .keyspace("rate_limits")
 *     .tableName("buckets")
 *     .keyColumn("bucket_key")
 *     .stateColumn("bucket_state")
 *     .versionColumn("bucket_version")
 *     .build();
 * }</pre>
 */
public class Bucket4jCassandra {

    /**
     * Returns the builder for {@link CassandraCompareAndSwapBasedProxyManager}
     *
     * @param session CQL session connected to the Cassandra cluster.
     *
     * @return new instance of {@link CassandraCompareAndSwapBasedProxyManagerBuilder}
     */
    public static CassandraCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(CqlSession session) {
        return new CassandraCompareAndSwapBasedProxyManagerBuilder<>(session, STRING);
    }

    /**
     * Returns the builder for {@link CassandraCompareAndSwapBasedProxyManager}
     *
     * @param session CQL session connected to the Cassandra cluster.
     * @param keyMapper object responsible for mapping keys from {@link K} to values of the partition key column.
     * @param <K> type of primary key
     *
     * @return new instance of {@link CassandraCompareAndSwapBasedProxyManagerBuilder}
     */
    public static <K> CassandraCompareAndSwapBasedProxyManagerBuilder<K> compareAndSwapBasedBuilder(CqlSession session, Mapper<K> keyMapper) {
        return new CassandraCompareAndSwapBasedProxyManagerBuilder<>(session, keyMapper);
    }

    public static class CassandraCompareAndSwapBasedProxyManagerBuilder<K> extends AbstractProxyManagerBuilder<K, CassandraCompareAndSwapBasedProxyManager<K>, CassandraCompareAndSwapBasedProxyManagerBuilder<K>> {

        private final CqlSession session;
        private Mapper<K> keyMapper;
        private String keyspace;
        private String tableName;
        private String keyColumn;
        private String stateColumn;
        private String versionColumn;
        private ConsistencyLevel readConsistencyLevel = DefaultConsistencyLevel.LOCAL_SERIAL;
        private ConsistencyLevel serialConsistencyLevel = DefaultConsistencyLevel.LOCAL_SERIAL;

        public CassandraCompareAndSwapBasedProxyManagerBuilder(CqlSession session, Mapper<K> keyMapper) {
            this.session = Objects.requireNonNull(session);
            this.keyMapper = Objects.requireNonNull(keyMapper);
        }

        /**
         * Specifies the keyspace that contains the table with buckets. Optional.
         *
         * <p>
         * When specified, all CQL statements refer the table as {@code keyspace.tableName},
         * otherwise the default keyspace of the session is used.
         *
         * @param keyspace name of Cassandra keyspace
         *
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> keyspace(String keyspace) {
            this.keyspace = Objects.requireNonNull(keyspace);
            return this;
        }

        /**
         * Specifies the table that stores buckets. Required.
         *
         * @param tableName name of Cassandra table
         *
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName);
            return this;
        }

        /**
         * Specifies the partition key column. Required.
         *
         * @param keyColumn the column that holds bucket key, it must be the {@code PRIMARY KEY} of the table
         *
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> keyColumn(String keyColumn) {
            this.keyColumn = Objects.requireNonNull(keyColumn);
            return this;
        }

        /**
         * Specifies the column that holds serialized bucket state. Required.
         *
         * @param stateColumn the {@code blob} column that holds serialized bucket state
         *
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> stateColumn(String stateColumn) {
            this.stateColumn = Objects.requireNonNull(stateColumn);
            return this;
        }

        /**
         * Specifies the column that is used as condition of Lightweight Transactions. Required.
         *
         * <p>
         * The version column holds monotonically increasing counter that is incremented by each successful write.
         * Conditional updates are guarded by this column instead of comparing the whole state blob,
         * that significantly reduces the amount of data which Paxos needs to evaluate.
         *
         * @param versionColumn the {@code bigint} column that holds the version counter
         *
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> versionColumn(String versionColumn) {
            this.versionColumn = Objects.requireNonNull(versionColumn);
            return this;
        }

        /**
         * Specifies the consistency level that is applied to the statement that reads bucket state.
         *
         * <p>
         * Defaults to {@link DefaultConsistencyLevel#LOCAL_SERIAL} because serial reads are the only way to
         * observe the result of the latest committed Lightweight Transaction, which in turn guarantees that
         * the compare-and-swap loop converges even when replicas lag behind.
         *
         * <p>
         * Non-serial levels like {@link DefaultConsistencyLevel#LOCAL_QUORUM} cost less, and they are still safe
         * because a stale read always leads to unsuccessful compare-and-swap instead of corrupted state,
         * but the number of retries can grow when replicas are not up to date.
         *
         * @param readConsistencyLevel consistency level for reading bucket state
         *
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> readConsistencyLevel(ConsistencyLevel readConsistencyLevel) {
            this.readConsistencyLevel = Objects.requireNonNull(readConsistencyLevel);
            return this;
        }

        /**
         * Specifies the serial consistency level that is applied to Lightweight Transactions.
         *
         * <p>
         * Defaults to {@link DefaultConsistencyLevel#LOCAL_SERIAL} that restricts Paxos coordination
         * to the local datacenter. Use {@link DefaultConsistencyLevel#SERIAL} only when cross-datacenter
         * coordination is required, for example in active-active multi-datacenter deployments.
         *
         * @param serialConsistencyLevel serial consistency level for conditional writes
         *
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> serialConsistencyLevel(ConsistencyLevel serialConsistencyLevel) {
            this.serialConsistencyLevel = Objects.requireNonNull(serialConsistencyLevel);
            return this;
        }

        /**
         * Specifies the type of key.
         *
         * @param keyMapper object responsible for converting primary keys to values of the partition key column.
         *
         * @return this builder instance
         */
        public <K2> CassandraCompareAndSwapBasedProxyManagerBuilder<K2> keyMapper(Mapper<K2> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (CassandraCompareAndSwapBasedProxyManagerBuilder<K2>) this;
        }

        public CqlSession getSession() {
            return session;
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
        }

        public String getKeyspace() {
            return keyspace;
        }

        public String getTableName() {
            return tableName;
        }

        public String getKeyColumn() {
            return keyColumn;
        }

        public String getStateColumn() {
            return stateColumn;
        }

        public String getVersionColumn() {
            return versionColumn;
        }

        public ConsistencyLevel getReadConsistencyLevel() {
            return readConsistencyLevel;
        }

        public ConsistencyLevel getSerialConsistencyLevel() {
            return serialConsistencyLevel;
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

        @Override
        public CassandraCompareAndSwapBasedProxyManager<K> build() {
            Objects.requireNonNull(tableName, "tableName must be specified via tableName(String)");
            Objects.requireNonNull(keyColumn, "keyColumn must be specified via keyColumn(String)");
            Objects.requireNonNull(stateColumn, "stateColumn must be specified via stateColumn(String)");
            Objects.requireNonNull(versionColumn, "versionColumn must be specified via versionColumn(String)");
            return new CassandraCompareAndSwapBasedProxyManager<>(this);
        }
    }
}