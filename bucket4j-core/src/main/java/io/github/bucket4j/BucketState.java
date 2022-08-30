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

import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.Scope;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.versioning.Version;

import java.io.IOException;
import java.util.Map;

public interface BucketState {

    BucketState copy();

    BucketConfiguration getConfiguration();

    void setConfiguration(BucketConfiguration configuration);

    BucketState replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy, long currentTimeNanos);

    void copyStateFrom(BucketState sourceState);

    long getAvailableTokens();

    void consume(long toConsume);

    long calculateDelayNanosAfterWillBePossibleToConsume(long tokensToConsume, long currentTimeNanos, boolean checkTokensToConsumeShouldBeLessThenCapacity);

    long calculateFullRefillingTime(long currentTimeNanos);

    void refillAllBandwidth(long currentTimeNanos);

    void addTokens(long tokensToAdd);

    void reset();

    void forceAddTokens(long tokensToAdd);

    long getCurrentSize(int bandwidth);

    long getRoundingError(int bandwidth);

    MathType getMathType();

    static BucketState createInitialState(BucketConfiguration configuration, MathType mathType, long currentTimeNanos) {
        switch (mathType) {
            case INTEGER_64_BITS: return new BucketState64BitsInteger(configuration, currentTimeNanos);
            case IEEE_754: return new BucketStateIEEE754(configuration, currentTimeNanos);
            default: throw new IllegalStateException("Unsupported mathType:" + mathType);
        }
    }

    static <S> BucketState deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
        int typeId = adapter.readInt(input);
        if (typeId == BucketState64BitsInteger.SERIALIZATION_HANDLE.getTypeId()) {
            return BucketState64BitsInteger.SERIALIZATION_HANDLE.deserialize(adapter, input);
        } else if (typeId == BucketStateIEEE754.SERIALIZATION_HANDLE.getTypeId()) {
            return BucketStateIEEE754.SERIALIZATION_HANDLE.deserialize(adapter, input);
        } else {
            throw new IOException("Unknown typeId=" + typeId);
        }
    }

    static <O> void serialize(SerializationAdapter<O> adapter, O output, BucketState state, Version backwardCompatibilityVersion, Scope scope) throws IOException {
        switch (state.getMathType()) {
            case INTEGER_64_BITS:
                adapter.writeInt(output, BucketState64BitsInteger.SERIALIZATION_HANDLE.getTypeId());
                BucketState64BitsInteger.SERIALIZATION_HANDLE.serialize(adapter, output, (BucketState64BitsInteger) state, backwardCompatibilityVersion, scope);
                break;
            case IEEE_754:
                adapter.writeInt(output, BucketStateIEEE754.SERIALIZATION_HANDLE.getTypeId());
                BucketStateIEEE754.SERIALIZATION_HANDLE.serialize(adapter, output, (BucketStateIEEE754) state, backwardCompatibilityVersion, scope);
                break;
            default:
                throw new IOException("Unknown mathType=" + state.getMathType());
        }
    }

    static BucketState fromJsonCompatibleSnapshot(Map<String, Object> snapshot) throws IOException {
        String type = (String) snapshot.get("type");
        if (BucketState64BitsInteger.SERIALIZATION_HANDLE.getTypeName().equals(type)) {
            return BucketState64BitsInteger.SERIALIZATION_HANDLE.fromJsonCompatibleSnapshot(snapshot);
        } else if (BucketStateIEEE754.SERIALIZATION_HANDLE.getTypeName().equals(type)) {
            return BucketStateIEEE754.SERIALIZATION_HANDLE.fromJsonCompatibleSnapshot(snapshot);
        } else {
            throw new IOException("Unknown typeName=" + type);
        }
    }

    static Object toJsonCompatibleSnapshot(BucketState state, Version backwardCompatibilityVersion, Scope scope) throws IOException {
        switch (state.getMathType()) {
            case INTEGER_64_BITS: {
                Map<String, Object> result = BucketState64BitsInteger.SERIALIZATION_HANDLE.toJsonCompatibleSnapshot((BucketState64BitsInteger) state, backwardCompatibilityVersion, scope);
                result.put("type", BucketState64BitsInteger.SERIALIZATION_HANDLE.getTypeName());
                return result;
            }
            case IEEE_754: {
                Map<String, Object> result = BucketStateIEEE754.SERIALIZATION_HANDLE.toJsonCompatibleSnapshot((BucketStateIEEE754) state, backwardCompatibilityVersion, scope);
                result.put("type", BucketStateIEEE754.SERIALIZATION_HANDLE.getTypeName());
                return result;
            }
            default:
                throw new IOException("Unknown mathType=" + state.getMathType());
        }
    }

}
