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

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.Function;

/**
 * Intention of this class is to provide wrapper around results returned by any method of {@link VerboseBucket} and {@link AsyncVerboseBucket}.
 */
public class VerboseResult<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long operationTimeNanos;
    private final T value;
    private final BucketConfiguration configuration;
    private final BucketState state;

    public VerboseResult(long operationTimeNanos, T value, BucketConfiguration configuration, BucketState state) {
        this.operationTimeNanos = operationTimeNanos;
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

    public <R extends Serializable> VerboseResult<R> map(Function<T, R> mapper) {
        return new VerboseResult<>(operationTimeNanos, mapper.apply(value), configuration, state);
    }

    public static final SerializationHandle<VerboseResult<?>> SERIALIZATION_HANDLE = new SerializationHandle<VerboseResult<?>>() {

        @Override
        public <I> VerboseResult<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            long operationTimeNanos = adapter.readLong(input);
            Serializable result = (Serializable) adapter.readObject(input);
            BucketConfiguration configuration = adapter.readObject(input, BucketConfiguration.class);
            BucketState state = adapter.readObject(input, BucketState.class);

            return new VerboseResult<>(operationTimeNanos, result, configuration, state);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, VerboseResult<?> result) throws IOException {
            adapter.writeLong(output, result.operationTimeNanos);
            adapter.writeObject(output, result.value);
            adapter.writeObject(output, result.configuration);
            adapter.writeObject(output, result.state);
        }

        @Override
        public int getTypeId() {
            return 24;
        }

        @Override
        public Class<VerboseResult<?>> getSerializedType() {
            return (Class) VerboseResult.class;
        }
    };

}
