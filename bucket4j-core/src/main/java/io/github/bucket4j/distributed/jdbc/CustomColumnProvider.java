/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2024 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
