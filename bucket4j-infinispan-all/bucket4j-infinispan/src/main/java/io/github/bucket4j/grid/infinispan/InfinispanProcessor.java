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
package io.github.bucket4j.grid.infinispan;

import java.io.Serial;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.util.ComparableByContent;
import org.infinispan.functional.EntryView;
import org.infinispan.functional.MetaParam;
import org.infinispan.util.function.SerializableFunction;

public class InfinispanProcessor<K, R> implements
        SerializableFunction<EntryView.ReadWriteEntryView<K, byte[]>, byte[]>,
        ComparableByContent<InfinispanProcessor> {

    @Serial
    private static final long serialVersionUID = 911L;

    private final byte[] requestBytes;

    public InfinispanProcessor(Request<R> request) {
        this.requestBytes = InternalSerializationHelper.serializeRequest(request);
    }

    public InfinispanProcessor(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    @Override
    public byte[] apply(EntryView.ReadWriteEntryView<K, byte[]> entry) {
        if (requestBytes.length == 0) {
            // it is the marker to remove bucket state
            if (entry.find().isPresent()) {
                entry.remove();
                return new byte[0];
            }
        }

        return new AbstractBinaryTransaction(requestBytes) {
            @Override
            public boolean exists() {
                return entry.find().isPresent();
            }

            @Override
            protected byte[] getRawState() {
                return entry.get();
            }

            @Override
            protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
                ExpirationAfterWriteStrategy expirationStrategy = getExpirationStrategy();
                long ttlMillis = expirationStrategy == null ? -1 : expirationStrategy.calculateTimeToLiveMillis(newState, getCurrentTimeNanos());
                if (ttlMillis > 0) {
                    entry.set(newStateBytes, new MetaParam.MetaLifespan(ttlMillis));
                } else {
                    entry.set(newStateBytes);
                }
            }
        }.execute();
    }

    @Override
    public boolean equalsByContent(InfinispanProcessor other) {
        return ComparableByContent.equals(requestBytes, other.requestBytes);
    }

    public byte[] getRequestBytes() {
        return requestBytes;
    }

}
