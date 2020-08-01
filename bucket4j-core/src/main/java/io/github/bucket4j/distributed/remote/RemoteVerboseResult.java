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
package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.VerboseBucket;
import io.github.bucket4j.VerboseResult;
import io.github.bucket4j.distributed.AsyncVerboseBucket;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.function.Function;

/**
 * Intention of this class is to provide wrapper around results returned by any method of {@link VerboseBucket} and {@link AsyncVerboseBucket}.
 */
public class RemoteVerboseResult<T> implements ComparableByContent<RemoteVerboseResult<?>> {

    private final long operationTimeNanos;
    private final int resultTypeId;
    private final T value;
    private final RemoteBucketState state;

    public RemoteVerboseResult(long operationTimeNanos, int resultTypeId, T value, RemoteBucketState state) {
        this.operationTimeNanos = operationTimeNanos;
        this.resultTypeId = resultTypeId;
        this.value = value;
        this.state = state;
    }

    /**
     * @return result of operation with bucket
     */
    public T getValue() {
        return value;
    }

    /**
     * @return snapshot of internal bucket state which was actual at operation time
     */
    public RemoteBucketState getState() {
        return state;
    }

    /**
     * @return time which was used by the bucket at the moment of handling a request
     */
    public long getOperationTimeNanos() {
        return operationTimeNanos;
    }

    public VerboseResult<T> asLocal() {
        return new VerboseResult<>(operationTimeNanos, value, state.getConfiguration(), state.copyBucketState());
    }

    public <R> RemoteVerboseResult<R> map(Function<T, R> mapper) {
        return new RemoteVerboseResult<R>(operationTimeNanos, resultTypeId, mapper.apply(value), state);
    }

    public static final SerializationHandle<RemoteVerboseResult<?>> SERIALIZATION_HANDLE = new SerializationHandle<RemoteVerboseResult<?>>() {

        @Override
        public <I> RemoteVerboseResult<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            long operationTimeNanos = adapter.readLong(input);

            int typeId = adapter.readInt(input);
            SerializationHandle handle = SerializationHandle.CORE_HANDLES.getHandleByTypeId(typeId);
            Object result = handle.deserialize(adapter, input);
            RemoteBucketState state = RemoteBucketState.SERIALIZATION_HANDLE.deserialize(adapter, input);
            return new RemoteVerboseResult(operationTimeNanos, typeId, result, state);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, RemoteVerboseResult<?> result) throws IOException {
            adapter.writeLong(output, result.operationTimeNanos);

            adapter.writeInt(output, result.resultTypeId);
            SerializationHandle handle = SerializationHandle.CORE_HANDLES.getHandleByTypeId(result.resultTypeId);
            handle.serialize(adapter, output, result.value);
            RemoteBucketState.SERIALIZATION_HANDLE.serialize(adapter, output, result.state);
        }

        @Override
        public int getTypeId() {
            return 14;
        }

        @Override
        public Class<RemoteVerboseResult<?>> getSerializedType() {
            return (Class) RemoteVerboseResult.class;
        }
    };

    @Override
    public boolean equalsByContent(RemoteVerboseResult<?> other) {
        return operationTimeNanos == other.operationTimeNanos
                && resultTypeId == other.resultTypeId
                && ComparableByContent.equals(value, other.value)
                && ComparableByContent.equals(state, other.state);
    }

}
