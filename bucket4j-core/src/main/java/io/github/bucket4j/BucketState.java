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

import io.github.bucket4j.core_algorithms.BucketState64BitsInteger;
import io.github.bucket4j.core_algorithms.BucketStateIEEE754;

import java.io.Serializable;

public interface BucketState extends Serializable {

    BucketState copy();

    void copyStateFrom(BucketState sourceState);

    long getAvailableTokens(Bandwidth[] bandwidths);

    void consume(Bandwidth[] bandwidths, long toConsume);

    long calculateDelayNanosAfterWillBePossibleToConsume(Bandwidth[] bandwidths, long tokensToConsume, long currentTimeNanos);

    void refillAllBandwidth(Bandwidth[] limits, long currentTimeNanos);

    void addTokens(Bandwidth[] bandwidths, long tokensToAdd);

    static BucketState createInitialState(BucketConfiguration configuration, long currentTimeNanos) {
        switch (configuration.getMathType()) {
            case INTEGER_64_BITS: return new BucketState64BitsInteger(configuration, currentTimeNanos);
            case IEEE_754: return new BucketStateIEEE754(configuration, currentTimeNanos);
            default: throw new IllegalStateException("Unsupported mathType:" + configuration.getMathType());
        }
    }

    long getCurrentSize(int bandwidth);

    long getRoundingError(int bandwidth);
}
