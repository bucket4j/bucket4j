package io.github.bucket4j.distributed.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

public interface CustomColumnProvider<K> {

    void setCustomField(K key, int paramIndex, PreparedStatement statement, RemoteBucketState state, long currentTimeNanos) throws SQLException;

    String getCustomFieldName();

    static <K> CustomColumnProvider<K> createExpiresInColumnProvider(String expiresAtColumn, ExpirationAfterWriteStrategy expiration) {
        return new CustomColumnProvider<>() {
            @Override
            public void setCustomField(K key, int paramIndex, PreparedStatement statement, RemoteBucketState state, long currentTimeNanos) throws SQLException {
                long ttlMillis = expiration.calculateTimeToLiveMillis(state, currentTimeNanos);
                statement.setLong(paramIndex, ttlMillis < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + ttlMillis);
            }
            @Override
            public String getCustomFieldName() {
                return expiresAtColumn;
            }
        };
    }

}
