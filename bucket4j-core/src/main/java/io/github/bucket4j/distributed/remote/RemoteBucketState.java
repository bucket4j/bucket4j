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
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.bucket4j.distributed.versioning.Versions.v_7_0_0;
import static io.github.bucket4j.distributed.versioning.Versions.v_7_6_0;


public class RemoteBucketState implements ComparableByContent<RemoteBucketState> {

    private BucketState state;
    private RemoteStat stat;
    private Long configurationVersion;

    public static final SerializationHandle<RemoteBucketState> SERIALIZATION_HANDLE = new SerializationHandle<RemoteBucketState>() {
        @Override
        public <S> RemoteBucketState deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_7_0_0, v_7_6_0);

            BucketConfiguration bucketConfiguration = BucketConfiguration.SERIALIZATION_HANDLE.deserialize(adapter, input);
            BucketState bucketState = BucketState.deserialize(adapter, input);
            bucketState.setConfiguration(bucketConfiguration);
            RemoteStat stat = RemoteStat.SERIALIZATION_HANDLE.deserialize(adapter, input);

            Long configurationVersion = null;
            if (formatNumber >= v_7_6_0.getNumber()) {
                boolean hasConfigurationVersion = adapter.readBoolean(input);
                if (hasConfigurationVersion) {
                    configurationVersion = adapter.readLong(input);
                }
            }
            return new RemoteBucketState(bucketState, stat, configurationVersion);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, RemoteBucketState remoteState, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Version serializationVersion;
            if (remoteState.configurationVersion == null) {
                serializationVersion = v_7_0_0;
            } else if (backwardCompatibilityVersion.getNumber() >= v_7_6_0.getNumber()) {
                serializationVersion = v_7_6_0;
            } else if (scope == Scope.RESPONSE) {
                serializationVersion = v_7_0_0;
            } else {
                throw new IllegalStateException("configurationVersion is not supported in format number " + backwardCompatibilityVersion.getNumber());
            }
            adapter.writeInt(output, serializationVersion.getNumber());

            BucketConfiguration.SERIALIZATION_HANDLE.serialize(adapter, output, remoteState.getConfiguration(), backwardCompatibilityVersion, scope);
            BucketState.serialize(adapter, output, remoteState.state, backwardCompatibilityVersion, scope);
            RemoteStat.SERIALIZATION_HANDLE.serialize(adapter, output, remoteState.stat, backwardCompatibilityVersion, scope);

            if (serializationVersion == v_7_6_0) {
                if (remoteState.configurationVersion != null) {
                    adapter.writeBoolean(output, true);
                    adapter.writeLong(output, remoteState.configurationVersion);
                } else {
                    adapter.writeBoolean(output, false);
                }
            }
        }

        @Override
        public int getTypeId() {
            return 5;
        }

        @Override
        public Class<RemoteBucketState> getSerializedType() {
            return RemoteBucketState.class;
        }

        @Override
        public RemoteBucketState fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
            int formatNumber = readIntValue(snapshot, "version");
            Versions.check(formatNumber, v_7_0_0, v_7_6_0);

            BucketState state = BucketState.fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("state"));
            RemoteStat stat = RemoteStat.SERIALIZATION_HANDLE.fromJsonCompatibleSnapshot((Map<String, Object>) snapshot.get("stat"));

            Long configurationVersion = null;
            if (formatNumber >= v_7_6_0.getNumber()) {
                configurationVersion = readOptionalLongValue(snapshot, "configurationVersion");
            }

            return new RemoteBucketState(state, stat, configurationVersion);
        }

        @Override
        public Map<String, Object> toJsonCompatibleSnapshot(RemoteBucketState remoteState, Version backwardCompatibilityVersion, Scope scope) throws IOException {
            Map<String, Object> result = new HashMap<>();
            Version serializationVersion;
            if (remoteState.configurationVersion == null) {
                serializationVersion = v_7_0_0;
            } else if (backwardCompatibilityVersion.getNumber() >= v_7_6_0.getNumber()) {
                serializationVersion = v_7_6_0;
            } else if (scope == Scope.RESPONSE) {
                serializationVersion = v_7_0_0;
            } else {
                throw new IllegalStateException("configurationVersion is not supported in format number " + backwardCompatibilityVersion.getNumber());
            }
            result.put("version", serializationVersion.getNumber());
            result.put("state", BucketState.toJsonCompatibleSnapshot(remoteState.state, backwardCompatibilityVersion, scope));
            result.put("stat", RemoteStat.SERIALIZATION_HANDLE.toJsonCompatibleSnapshot(remoteState.stat, backwardCompatibilityVersion, scope));
            if (serializationVersion == v_7_6_0) {
                result.put("configurationVersion", remoteState.configurationVersion);
            }
            return result;
        }

        @Override
        public String getTypeName() {
            return "RemoteBucketState";
        }

    };

    public RemoteBucketState(BucketState state, RemoteStat stat, Long configurationVersion) {
        this.state = state;
        this.stat = stat;
        this.configurationVersion = configurationVersion;
    }

    public void refillAllBandwidth(long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);
    }

    public long getAvailableTokens() {
        return state.getAvailableTokens();
    }

    public void consume(long tokensToConsume) {
        state.consume(tokensToConsume);
        stat.addConsumedTokens(tokensToConsume);
    }

    public long calculateFullRefillingTime(long currentTimeNanos) {
        return state.calculateFullRefillingTime(currentTimeNanos);
    }

    public long calculateDelayNanosAfterWillBePossibleToConsume(long tokensToConsume, long currentTimeNanos, boolean checkTokensToConsumeShouldBeLessThenCapacity) {
        return state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos, checkTokensToConsumeShouldBeLessThenCapacity);
    }

    public void addTokens(long tokensToAdd) {
        state.addTokens(tokensToAdd);
    }

    public void forceAddTokens(long tokensToAdd) {
        state.forceAddTokens(tokensToAdd);
    }

    public void reset() {
        state.reset();
    }

    public BucketState copyBucketState() {
        return state.copy();
    }

    public void replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy, long currentTimeNanos) {
        state = state.replaceConfiguration(newConfiguration, tokensInheritanceStrategy, currentTimeNanos);
    }

    public BucketConfiguration getConfiguration() {
        return state.getConfiguration();
    }

    public RemoteStat getRemoteStat() {
        return stat;
    }

    public BucketState getState() {
        return state;
    }

    public Long getConfigurationVersion() {
        return configurationVersion;
    }

    @Override
    public boolean equalsByContent(RemoteBucketState other) {
        return ComparableByContent.equals(state, other.state) &&
                ComparableByContent.equals(state.getConfiguration(), other.getConfiguration()) &&
                ComparableByContent.equals(stat, other.stat);
    }

    public RemoteBucketState copy() {
        return new RemoteBucketState(state.copy(), stat.copy(), configurationVersion);
    }

    @Override
    public String toString() {
        return "RemoteBucketState{" +
                "state=" + state +
                ", stat=" + stat +
                ", configurationVersion=" + configurationVersion +
                '}';
    }

}
