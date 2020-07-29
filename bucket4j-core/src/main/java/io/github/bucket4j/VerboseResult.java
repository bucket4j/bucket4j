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
import io.github.bucket4j.util.ComparableByContent;
import java.util.function.Function;

/**
 * Intention of this class is to provide wrapper around results returned by any method of {@link VerboseBucket} and {@link AsyncVerboseBucket}.
 */
public class VerboseResult<T> implements ComparableByContent<VerboseResult<?>> {

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

    public <R> VerboseResult<R> map(Function<T, R> mapper) {
        return new VerboseResult<R>(operationTimeNanos, mapper.apply(value), configuration, state);
    }

    @Override
    public boolean equalsByContent(VerboseResult<?> other) {
        return operationTimeNanos == other.operationTimeNanos
            && ComparableByContent.equals(value, other.value)
            && configuration.equalsByContent(other.configuration)
            && ComparableByContent.equals(state, other.state);
    }

}
