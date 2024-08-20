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

import java.util.function.Function;

import io.github.bucket4j.distributed.AsyncVerboseBucket;
import io.github.bucket4j.util.ComparableByContent;

/**
 * Intention of this class is to provide wrapper around results returned by any method of {@link VerboseBucket} and {@link AsyncVerboseBucket}.
 */
public class VerboseResult<T> implements ComparableByContent<VerboseResult<?>> {

    private final long operationTimeNanos;
    private final T value;
    private final BucketState state;

    public VerboseResult(long operationTimeNanos, T value, BucketState state) {
        this.operationTimeNanos = operationTimeNanos;
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
     * @return snapshot of configuration which was actual at operation time
     */
    public BucketConfiguration getConfiguration() {
        return state.getConfiguration();
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

    /**
     * @return internal state describer
     */
    public Diagnostics getDiagnostics() {
        return new Diagnostics() {
            @Override
            public long calculateFullRefillingTime() {
                return state.calculateFullRefillingTime(operationTimeNanos);
            }
            @Override
            public long getAvailableTokens() {
                return state.getAvailableTokens();
            }

            @Override
            public long[] getAvailableTokensPerEachBandwidth() {
                Bandwidth[] bandwidths = state.getConfiguration().getBandwidths();
                long[] availableTokens = new long[bandwidths.length];
                for (int i = 0; i < bandwidths.length; i++) {
                    availableTokens[i] = state.getCurrentSize(i);
                }
                return availableTokens;
            }
        };
    }

    /**
     * Describer of internal bucket state
     */
    public interface Diagnostics {

        /**
         * Returns time in nanoseconds that need to wait until bucket will be fully refilled to its maximum
         *
         * @return time in nanoseconds that need to wait until bucket will be fully refilled to its maximum
         */
        long calculateFullRefillingTime();

        /**
         * Returns currently available tokens
         *
         * @return currently available tokens
         */
        long getAvailableTokens();

        /**
         * Returns currently available tokens per each bandwidth.
         * Element's order inside resulted array depends on from order in which bandwidth is specified inside {@link BucketConfiguration}.
         *
         * @return currently available tokens per each bandwidth
         */
        long[] getAvailableTokensPerEachBandwidth();

    }

    public <R> VerboseResult<R> map(Function<T, R> mapper) {
        return new VerboseResult<>(operationTimeNanos, mapper.apply(value), state);
    }

    public <R> VerboseResult<R> withValue(R newValue) {
        return new VerboseResult<>(operationTimeNanos, newValue, state);
    }

    @Override
    public boolean equalsByContent(VerboseResult<?> other) {
        return operationTimeNanos == other.operationTimeNanos
            && ComparableByContent.equals(value, other.value)
            && state.getConfiguration().equalsByContent(other.getState().getConfiguration())
            && ComparableByContent.equals(state, other.state);
    }

}
