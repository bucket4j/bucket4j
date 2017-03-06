/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j.grid;

import com.github.bucket4j.Bandwidth;
import com.github.bucket4j.BucketConfiguration;
import com.github.bucket4j.BucketState;

public class ConsumeOrCalculateTimeToCloseDeficitCommand implements GridCommand<Long> {

    private long tokensToConsume;
    private boolean bucketStateModified;

    public ConsumeOrCalculateTimeToCloseDeficitCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public Long execute(GridBucketState gridState) {
        BucketConfiguration configuration = gridState.getBucketConfiguration();
        BucketState state = gridState.getBucketState();
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();
        Bandwidth[] limits = configuration.getLimitedBandwidths();
        Bandwidth guarantee = configuration.getGuaranteedBandwidth();
        state.refillAllBandwidth(limits, guarantee, currentTimeNanos);
        long timeToCloseDeficit = state.delayNanosAfterWillBePossibleToConsume(limits, guarantee, currentTimeNanos, tokensToConsume);
        if (timeToCloseDeficit == 0) {
            state.consume(limits, guarantee, tokensToConsume);
            bucketStateModified = true;
        }
        return timeToCloseDeficit;
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

}
