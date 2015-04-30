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
import com.github.bandwidthlimiter.bucket.Bandwidth;
import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;

public class NonSynchronizedBucket extends AbstractBucket {

    private final BucketState state;

    public NonSynchronizedBucket(BucketConfiguration configuration) {
        super(configuration);
        this.state = BucketState.createInitialState(configuration);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        long currentTime = configuration.getTimeMeter().currentTime();
        Bandwidth[] bandwidths = configuration.getBandwidths();
        state.refill(bandwidths, currentTime);
        long availableToConsume = state.getAvailableTokens(bandwidths);
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume == 0) {
            return 0;
        }
        state.consume(bandwidths, toConsume);
        return toConsume;
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        long currentTime = configuration.getTimeMeter().currentTime();
        Bandwidth[] bandwidths = configuration.getBandwidths();
        state.refill(bandwidths, currentTime);
        long availableToConsume = state.getAvailableTokens(bandwidths);
        if (tokensToConsume <= availableToConsume) {
            state.consume(bandwidths, tokensToConsume);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyTimeLimit) throws InterruptedException {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        boolean isWaitingLimited = waitIfBusyTimeLimit > 0;

        final long methodStartTime = isWaitingLimited? configuration.getTimeMeter().currentTime(): 0;
        long currentTime = methodStartTime;
        long methodDuration = 0;
        boolean isFirstCycle = true;

        while (true) {
            if (isFirstCycle) {
                isFirstCycle = false;
            } else {
                currentTime = configuration.getTimeMeter().currentTime();
                methodDuration = currentTime - methodStartTime;
                if (isWaitingLimited && methodDuration >= waitIfBusyTimeLimit) {
                    return false;
                }
            }

            state.refill(bandwidths, currentTime);
            long timeToCloseDeficit = state.delayAfterWillBePossibleToConsume(bandwidths, currentTime, tokensToConsume);
            if (timeToCloseDeficit == Long.MAX_VALUE) {
                return false;
            }
            if (timeToCloseDeficit == 0) {
                state.consume(bandwidths, tokensToConsume);
                return true;
            }

            if (isWaitingLimited) {
                long sleepingTimeLimit = waitIfBusyTimeLimit - methodDuration;
                if (timeToCloseDeficit >= sleepingTimeLimit) {
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