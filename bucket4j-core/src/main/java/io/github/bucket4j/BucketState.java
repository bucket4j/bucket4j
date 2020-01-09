/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;

import java.io.IOException;
import java.io.Serializable;

public interface BucketState extends Serializable {

    BucketState copy();

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

    static <O> void serialize(SerializationAdapter<O> adapter, O output, BucketState state) throws IOException {
        switch (state.getMathType()) {
            case INTEGER_64_BITS:
                adapter.writeInt(output, BucketState64BitsInteger.SERIALIZATION_HANDLE.getTypeId());
                BucketState64BitsInteger.SERIALIZATION_HANDLE.serialize(adapter, output, (BucketState64BitsInteger) state);
                break;
            case IEEE_754:
                adapter.writeInt(output, BucketStateIEEE754.SERIALIZATION_HANDLE.getTypeId());
                BucketStateIEEE754.SERIALIZATION_HANDLE.serialize(adapter, output, (BucketStateIEEE754) state);
                break;
            default:
                throw new IOException("Unknown mathType=" + state.getMathType());
        }
    }


}
