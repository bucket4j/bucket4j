/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.hazelcast;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.ExtendedMapEntry;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.util.ComparableByContent;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

public class HazelcastEntryProcessor<K, T> implements EntryProcessor<K, byte[], byte[]>, ComparableByContent<HazelcastEntryProcessor> {

    private static final long serialVersionUID = 1L;

    private final byte[] requestBytes;
    private EntryProcessor<K, byte[], byte[]> backupProcessor;

    public HazelcastEntryProcessor(Request<T> request) {
        this.requestBytes = serializeRequest(request);
    }

    public HazelcastEntryProcessor(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    @Override
    public byte[] process(Map.Entry<K, byte[]> entry) {
        return new AbstractBinaryTransaction(requestBytes) {
            @Override
            public boolean exists() {
                return entry.getValue() != null;
            }

            @Override
            protected byte[] getRawState() {
                return entry.getValue();
            }

            @Override
            protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
                ExpirationAfterWriteStrategy expirationStrategy = getExpirationStrategy();
                long ttlMillis = expirationStrategy == null ? -1 : expirationStrategy.calculateTimeToLiveMillis(newState, getCurrentTimeNanos());
                if (ttlMillis > 0) {
                    entry.setValue(newStateBytes);
                    backupProcessor = new SimpleBackupProcessor<>(newStateBytes);
                } else {
                    ExtendedMapEntry extendedEntry = (ExtendedMapEntry) entry;
                    extendedEntry.setValue(newStateBytes, ttlMillis, TimeUnit.MILLISECONDS);
                    backupProcessor = new VersionedBackupProcessor<>(newStateBytes, ttlMillis);
                }
            }
        }.execute();
    }

    @Override
    public EntryProcessor<K, byte[], byte[]> getBackupProcessor() {
        return backupProcessor;
    }

    public byte[] getRequestBytes() {
        return requestBytes;
    }

    @Override
    public boolean equalsByContent(HazelcastEntryProcessor other) {
        return Arrays.equals(requestBytes, other.requestBytes);
    }

}
