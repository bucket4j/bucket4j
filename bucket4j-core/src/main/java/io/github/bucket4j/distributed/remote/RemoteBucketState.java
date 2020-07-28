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

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;


public class RemoteBucketState implements ComparableByContent<RemoteBucketState> {

    private BucketConfiguration configuration;
    private BucketState state;

    public static final SerializationHandle<RemoteBucketState> SERIALIZATION_HANDLE = new SerializationHandle<RemoteBucketState>() {
        @Override
        public <S> RemoteBucketState deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            BucketConfiguration bucketConfiguration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input);
            BucketState bucketState = BucketState.deserialize(adapter, input);
            return new RemoteBucketState(bucketConfiguration, bucketState);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, RemoteBucketState gridState) throws IOException {
            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, gridState.configuration);
            BucketState.serialize(adapter, output, gridState.state);
        }

        @Override
        public int getTypeId() {
            return 5;
        }

        @Override
        public Class<RemoteBucketState> getSerializedType() {
            return RemoteBucketState.class;
        }

    };

    public RemoteBucketState(BucketConfiguration configuration, BucketState state) {
        this.configuration = configuration;
        this.state = state;
    }

    public void refillAllBandwidth(long currentTimeNanos) {
        state.refillAllBandwidth(configuration.getBandwidths(), currentTimeNanos);
    }

    public long getAvailableTokens() {
        return state.getAvailableTokens(configuration.getBandwidths());
    }

    public void consume(long tokensToConsume) {
        state.consume(configuration.getBandwidths(), tokensToConsume);
    }

    public long calculateFullRefillingTime(long currentTimeNanos) {
        return state.calculateFullRefillingTime(configuration.getBandwidths(), currentTimeNanos);
    }

    public long calculateDelayNanosAfterWillBePossibleToConsume(long tokensToConsume, long currentTimeNanos) {
        return state.calculateDelayNanosAfterWillBePossibleToConsume(configuration.getBandwidths(), tokensToConsume, currentTimeNanos);
    }

    public void addTokens(long tokensToAdd) {
        state.addTokens(configuration.getBandwidths(), tokensToAdd);
    }

    public BucketState copyBucketState() {
        return state.copy();
    }

    public BucketConfiguration replaceConfigurationOrReturnPrevious(BucketConfiguration newConfiguration) {
        if (!configuration.isCompatible(newConfiguration)) {
            return configuration;
        }
        configuration = newConfiguration;
        return null;
    }

    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    public BucketState getState() {
        return state;
    }

    @Override
    public boolean equalsByContent(RemoteBucketState other) {
        return ComparableByContent.equals(state, other.state) &&
                ComparableByContent.equals(configuration, other.configuration);
    }

    public RemoteBucketState copy() {
        return new RemoteBucketState(configuration, state.copy());
    }

}
