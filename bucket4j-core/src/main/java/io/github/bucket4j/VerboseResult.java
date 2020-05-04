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
package io.github.bucket4j;

import io.github.bucket4j.distributed.AsyncVerboseBucket;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.util.function.Function;

/**
 * Intention of this class is to provide wrapper around results returned by any method of {@link VerboseBucket} and {@link AsyncVerboseBucket}.
 */
public class VerboseResult<T> {

    private final long operationTimeNanos;
    private final int resultTypeId;
    private final T value;
    private final BucketConfiguration configuration;
    private final BucketState state;

    public VerboseResult(long operationTimeNanos, int resultTypeId, T value, BucketConfiguration configuration, BucketState state) {
        this.operationTimeNanos = operationTimeNanos;
        this.resultTypeId = resultTypeId;
        this.value = value;
        this.configuration = configuration;
        this.state = state;
    }

    /**
     * @return result of operation with bucket
     */
    public T getValue() {
        return value;
    }

    /**
     * @return snapshot of configuration which was actual at operation time
     */
    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @return snapshot of internal bucket state which was actual at operation time
     */
    public BucketState getState() {
        return state;
    }

    /**
     * @return time which was used by the bucket at the moment of handling a request
     */
    public long getOperationTimeNanos() {
        return operationTimeNanos;
    }

    public <R> VerboseResult<R> map(Function<T, R> mapper) {
        return new VerboseResult<R>(operationTimeNanos, resultTypeId, mapper.apply(value), configuration, state);
    }

    public static final SerializationHandle<VerboseResult<?>> SERIALIZATION_HANDLE = new SerializationHandle<VerboseResult<?>>() {

        @Override
        public <I> VerboseResult<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            long operationTimeNanos = adapter.readLong(input);

            int typeId = adapter.readInt(input);
            SerializationHandle handle = SerializationHandle.CORE_HANDLES.getHandleByTypeId(typeId);
            Object result = handle.deserialize(adapter, input);
            BucketConfiguration configuration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input);
            BucketState state = BucketState.deserialize(adapter, input);

            return new VerboseResult(operationTimeNanos, typeId, result, configuration, state);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, VerboseResult<?> result) throws IOException {
            adapter.writeLong(output, result.operationTimeNanos);

            adapter.writeInt(output, result.resultTypeId);
            SerializationHandle handle = SerializationHandle.CORE_HANDLES.getHandleByTypeId(result.resultTypeId);
            handle.serialize(adapter, output, result.value);
            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, result.configuration);
            BucketState.serialize(adapter, output, result.state);
        }

        @Override
        public int getTypeId() {
            return 14;
        }

        @Override
        public Class<VerboseResult<?>> getSerializedType() {
            return (Class) VerboseResult.class;
        }
    };

}
