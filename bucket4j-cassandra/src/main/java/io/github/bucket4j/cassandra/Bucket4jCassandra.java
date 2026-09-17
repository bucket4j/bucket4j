package io.github.bucket4j.cassandra;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.serialization.Mapper;

import java.util.Objects;

import static io.github.bucket4j.distributed.serialization.Mapper.STRING;

/**
 * Entry point for Apache Cassandra integration using the DataStax Java Driver 4.x.
 *
 * <p>Schema fields ({@code tableName}, {@code keyColumn}, {@code stateColumn}, {@code versionColumn})
 * must be provided explicitly — no defaults are assumed, so the builder maps precisely to your schema.
 * {@code keyspace} is optional and only needed when the session has no default keyspace or you want
 * to target a different keyspace.
 *
 * <pre>{@code
 * ProxyManager<String> proxyManager = Bucket4jCassandra
 *     .compareAndSwapBasedBuilder(session)
 *     .keyspace("my_keyspace")      // optional — omit if session already has a default keyspace
 *     .tableName("rate_limits")
 *     .keyColumn("user_id")
 *     .stateColumn("bucket_state")
 *     .versionColumn("cas_version")
 *     .build();
 * }</pre>
 *
 * <p>Example Cassandra schema matching the values above:
 * <pre>{@code
 * CREATE KEYSPACE my_keyspace WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
 *
 * CREATE TABLE my_keyspace.rate_limits (
 *     user_id      text    PRIMARY KEY,
 *     bucket_state blob,
 *     cas_version  bigint
 * );
 * }</pre>
 */
public class Bucket4jCassandra {

    /**
     * Returns a builder for {@link CassandraCompareAndSwapBasedProxyManager} with {@code String} keys.
     *
     * @param session CQL session connected to the Cassandra cluster
     * @return new instance of {@link CassandraCompareAndSwapBasedProxyManagerBuilder}
     */
    public static CassandraCompareAndSwapBasedProxyManagerBuilder<String> compareAndSwapBasedBuilder(CqlSession session) {
        return new CassandraCompareAndSwapBasedProxyManagerBuilder<>(session, STRING);
    }

    /**
     * Returns a builder for {@link CassandraCompareAndSwapBasedProxyManager} with a custom key type.
     *
     * @param session   CQL session connected to the Cassandra cluster
     * @param keyMapper object responsible for mapping keys from {@code K} to Cassandra partition key values
     * @param <K>       type of primary key
     * @return new instance of {@link CassandraCompareAndSwapBasedProxyManagerBuilder}
     */
    public static <K> CassandraCompareAndSwapBasedProxyManagerBuilder<K> compareAndSwapBasedBuilder(CqlSession session, Mapper<K> keyMapper) {
        return new CassandraCompareAndSwapBasedProxyManagerBuilder<>(session, keyMapper);
    }

    public static class CassandraCompareAndSwapBasedProxyManagerBuilder<K>
            extends AbstractProxyManagerBuilder<K, CassandraCompareAndSwapBasedProxyManager<K>, CassandraCompareAndSwapBasedProxyManagerBuilder<K>> {

        private final CqlSession session;
        private Mapper<K> keyMapper;
        private String keyspace;
        private String tableName;
        private String keyColumn;
        private String stateColumn;
        private String versionColumn;
        private ConsistencyLevel serialConsistencyLevel = DefaultConsistencyLevel.LOCAL_SERIAL;

        public CassandraCompareAndSwapBasedProxyManagerBuilder(CqlSession session, Mapper<K> keyMapper) {
            this.session   = Objects.requireNonNull(session);
            this.keyMapper = Objects.requireNonNull(keyMapper);
        }

        public CqlSession getSession() {
            return session;
        }

        public String getKeyspace() {
            return keyspace;
        }

        public Mapper<K> getKeyMapper() {
            return keyMapper;
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

        public ConsistencyLevel getSerialConsistencyLevel() {
            return serialConsistencyLevel;
        }

        /**
         * Specifies the Cassandra keyspace that contains the bucket table. Optional.
         *
         * <p>When set, all CQL statements use the fully-qualified reference {@code keyspace.tableName}.
         * When omitted, the session's default keyspace is used (set via
         * {@code CqlSession.builder().withKeyspace(...)}).
         *
         * <p>Provide this when the session has no default keyspace, or when you want to target
         * a keyspace different from the session default.
         *
         * @param keyspace name of the Cassandra keyspace
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> keyspace(String keyspace) {
            this.keyspace = Objects.requireNonNull(keyspace);
            return this;
        }

        /**
         * Specifies the Cassandra table that stores bucket state. Required.
         *
         * @param tableName name of the Cassandra table
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName);
            return this;
        }

        /**
         * Specifies the partition key column name. Required.
         *
         * <p>Must match the table's {@code PRIMARY KEY} column (e.g. {@code "user_id"}, {@code "api_key"}).
         *
         * @param keyColumn column that holds the bucket key (must be the table's {@code PRIMARY KEY})
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> keyColumn(String keyColumn) {
            this.keyColumn = Objects.requireNonNull(keyColumn);
            return this;
        }

        /**
         * Specifies the state blob column name. Required.
         *
         * @param stateColumn column that holds the serialized bucket state ({@code blob})
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> stateColumn(String stateColumn) {
            this.stateColumn = Objects.requireNonNull(stateColumn);
            return this;
        }

        /**
         * Specifies the version column name used as the Lightweight Transaction (LWT) guard. Required.
         *
         * <p>The version column holds a monotonically incrementing {@code bigint} that is
         * incremented on every successful write. LWT conditions guard on this column
         * ({@code IF version = ?}) rather than comparing the full state blob, which reduces
         * Paxos condition evaluation overhead.
         *
         * @param versionColumn column that holds the CAS version counter ({@code bigint})
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> versionColumn(String versionColumn) {
            this.versionColumn = Objects.requireNonNull(versionColumn);
            return this;
        }

        /**
         * Specifies the serial consistency level for Lightweight Transaction (LWT) statements.
         *
         * <p>Defaults to {@link DefaultConsistencyLevel#LOCAL_SERIAL}, which restricts Paxos
         * coordination to the local datacenter — the recommended setting for single-DC or
         * single-region deployments, roughly halving LWT latency vs full
         * {@link DefaultConsistencyLevel#SERIAL}.
         *
         * <p>Use {@link DefaultConsistencyLevel#SERIAL} only when strict cross-DC LWT
         * coordination is required (active-active multi-DC deployments).
         *
         * @param serialConsistencyLevel serial consistency level for LWT operations
         * @return this builder instance
         */
        public CassandraCompareAndSwapBasedProxyManagerBuilder<K> serialConsistencyLevel(ConsistencyLevel serialConsistencyLevel) {
            this.serialConsistencyLevel = Objects.requireNonNull(serialConsistencyLevel);
            return this;
        }

        /**
         * Changes the key type.
         *
         * @param keyMapper object responsible for converting primary keys to Cassandra partition key values
         * @param <K2>      new key type
         * @return this builder instance
         */
        public <K2> CassandraCompareAndSwapBasedProxyManagerBuilder<K2> keyMapper(Mapper<K2> keyMapper) {
            this.keyMapper = (Mapper) Objects.requireNonNull(keyMapper);
            return (CassandraCompareAndSwapBasedProxyManagerBuilder<K2>) this;
        }

        @Override
        public boolean isExpireAfterWriteSupported() {
            return true;
        }

        @Override
        public CassandraCompareAndSwapBasedProxyManager<K> build() {
            Objects.requireNonNull(tableName,    "tableName must be specified via .tableName(...)");
            Objects.requireNonNull(keyColumn,    "keyColumn must be specified via .keyColumn(...)");
            Objects.requireNonNull(stateColumn,  "stateColumn must be specified via .stateColumn(...)");
            Objects.requireNonNull(versionColumn,"versionColumn must be specified via .versionColumn(...)");
            return new CassandraCompareAndSwapBasedProxyManager<>(this);
        }
    }
}
