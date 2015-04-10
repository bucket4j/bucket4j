/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.bandwidthlimiter.bucket.local;

import com.github.bandwidthlimiter.bucket.AbstractBucket;
import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;

public class UnsafeBucket extends AbstractBucket {

    private final BucketLocalState state;

    public UnsafeBucket(BucketConfiguration configuration) {
        super(configuration);
        this.state = new BucketLocalState(configuration);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        long currentTime = configuration.getTimeMeter().currentTime();
        configuration.getRefillStrategy().refill(configuration, state, currentTime);
        long availableToConsume = state.getAvailableTokens(configuration);
        long toConsume = Math.min(limit, availableToConsume);
        state.consume(configuration, toConsume);
        return toConsume;
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        long currentTime = configuration.getTimeMeter().currentTime();
        configuration.getRefillStrategy().refill(configuration, state, currentTime);
        long availableToConsume = state.getAvailableTokens(configuration);
        if (tokensToConsume <= availableToConsume) {
            state.consume(configuration, tokensToConsume);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyTimeLimit) throws InterruptedException {
        while (true) {
            long currentTime = configuration.getTimeMeter().currentTime();
            configuration.getRefillStrategy().refill(configuration, state, currentTime);
            long availableToConsume = state.getAvailableTokens(configuration);
            if (tokensToConsume <= availableToConsume) {
                state.consume(configuration, tokensToConsume);
                return true;
            }

            long deficitTokens = tokensToConsume - availableToConsume;
            long timeToCloseDeficit = state.calculateTimeToCloseDeficit(configuration, deficitTokens);
            if (waitIfBusyTimeLimit > 0) {
                if (timeToCloseDeficit > waitIfBusyTimeLimit) {
                    return false;
                }
            }
            configuration.getTimeMeter().sleep(timeToCloseDeficit);
        }
    }

    @Override
    public BucketState createSnapshot() {
        return state.clone();
    }

}