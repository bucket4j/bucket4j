/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2021 Vladimir Bukhtoyarov
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
package io.github.bucket4j.grid.ignite.thin.compute;

import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;
import org.apache.ignite.cache.CacheEntryProcessor;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import java.io.Serial;
import java.io.Serializable;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

public class IgniteEntryProcessor<K> implements Serializable, CacheEntryProcessor<K, byte[], byte[]> {

    @Serial
    private static final long serialVersionUID = 1;

    private final byte[] requestBytes;

    IgniteEntryProcessor(Request<?> request) {
        this.requestBytes = serializeRequest(request);
    }

    @Override
    public byte[] process(MutableEntry<K, byte[]> entry, Object... arguments) throws EntryProcessorException {
        return new AbstractBinaryTransaction(requestBytes) {
            @Override
            public boolean exists() {
                return entry.exists();
            }

            @Override
            protected byte[] getRawState() {
                return entry.getValue();
            }

            @Override
            protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
                entry.setValue(newStateBytes);
            }
        }.execute();
    }

}
