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
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.versioning.Version;

import java.io.IOException;

public interface BucketState {

    BucketState copy();

    BucketState replaceConfiguration(BucketConfiguration previousConfiguration, BucketConfiguration newConfiguration,
                                     TokensInheritanceStrategy tokensInheritanceStrategy, long currentTimeNanos);

    void copyStateFrom(BucketState sourceState);

    long getAvailableTokens(Bandwidth[] bandwidths);

    void consume(Bandwidth[] bandwidths, long toConsume);

    long calculateDelayNanosAfterWillBePossibleToConsume(Bandwidth[] bandwidths, long tokensToConsume, long currentTimeNanos);

    long calculateFullRefillingTime(Bandwidth[] bandwidths, long currentTimeNanos);

    void refillAllBandwidth(Bandwidth[] limits, long currentTimeNanos);

    void addTokens(Bandwidth[] bandwidths, long tokensToAdd);

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

    static <S> BucketState deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
        int typeId = adapter.readInt(input);
        if (typeId == BucketState64BitsInteger.SERIALIZATION_HANDLE.getTypeId()) {
            return BucketState64BitsInteger.SERIALIZATION_HANDLE.deserialize(adapter, input, backwardCompatibilityVersion);
        } else if (typeId == BucketStateIEEE754.SERIALIZATION_HANDLE.getTypeId()) {
            return BucketStateIEEE754.SERIALIZATION_HANDLE.deserialize(adapter, input, backwardCompatibilityVersion);
        } else {
            throw new IOException("Unknown typeId=" + typeId);
        }
    }

    static <O> void serialize(SerializationAdapter<O> adapter, O output, BucketState state, Version backwardCompatibilityVersion) throws IOException {
        switch (state.getMathType()) {
            case INTEGER_64_BITS:
                adapter.writeInt(output, BucketState64BitsInteger.SERIALIZATION_HANDLE.getTypeId());
                BucketState64BitsInteger.SERIALIZATION_HANDLE.serialize(adapter, output, (BucketState64BitsInteger) state, backwardCompatibilityVersion);
                break;
            case IEEE_754:
                adapter.writeInt(output, BucketStateIEEE754.SERIALIZATION_HANDLE.getTypeId());
                BucketStateIEEE754.SERIALIZATION_HANDLE.serialize(adapter, output, (BucketStateIEEE754) state, backwardCompatibilityVersion);
                break;
            default:
                throw new IOException("Unknown mathType=" + state.getMathType());
        }
    }

}
