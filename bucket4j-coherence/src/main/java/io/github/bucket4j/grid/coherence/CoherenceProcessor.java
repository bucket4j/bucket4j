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
package io.github.bucket4j.grid.coherence;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.util.ComparableByContent;

import java.io.Serial;
import java.util.Arrays;


public class CoherenceProcessor<K, T> extends AbstractProcessor<K, byte[], byte[]> implements ComparableByContent {

    @Serial
    @Serial
    private static final long serialVersionUID = 1L;

    private final byte[] requestBytes;

    public CoherenceProcessor(Request<T> request) {
        this.requestBytes = InternalSerializationHelper.serializeRequest(request);
    }

    public CoherenceProcessor(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    public byte[] getRequestBytes() {
        return requestBytes;
    }

    @Override
    public byte[] process(InvocableMap.Entry<K, byte[]> entry) {
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
                entry.setValue(newStateBytes);
            }
        }.execute();
    }


    @Override
    public boolean equalsByContent(ComparableByContent other) {
        CoherenceProcessor processor = (CoherenceProcessor) other;
        return Arrays.equals(requestBytes, processor.requestBytes);
    }

}
