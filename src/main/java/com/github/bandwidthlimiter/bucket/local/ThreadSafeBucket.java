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

import java.util.concurrent.atomic.AtomicReference;

public class ThreadSafeBucket extends AbstractBucket {

    private final AtomicReference<BucketLocalState> stateReference;
    private final BucketConfiguration configuration;

    public ThreadSafeBucket(BucketConfiguration configuration) {
        super(configuration);
        this.configuration = configuration;
        BucketLocalState initialState = new BucketLocalState(configuration);
        this.stateReference = new AtomicReference<>(initialState);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        BucketLocalState previousState = stateReference.get();
        BucketLocalState newState = previousState.clone();
        while (true) {
            long currentTime = configuration.getTimeMeter().currentTime();
            configuration.getRefillStrategy().refill(configuration, newState, currentTime);
            long availableToConsume = newState.getAvailableTokens(configuration);
            long toConsume = Math.min(limit, availableToConsume);
            newState.consume(configuration, toConsume);
            if (stateReference.compareAndSet(previousState, newState)) {
                return toConsume;
            } else {
                previousState = stateReference.get();
                newState.copyState(previousState);
            }
        }
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        BucketLocalState previousState = stateReference.get();
        BucketLocalState newState = previousState.clone();

        while (true) {
            long currentTime = configuration.getTimeMeter().currentTime();
            configuration.getRefillStrategy().refill(configuration, newState, currentTime);
            long availableToConsume = newState.getAvailableTokens(configuration);
            if (tokensToConsume > availableToConsume) {
                return false;
            }
            newState.consume(configuration, tokensToConsume);
            if (stateReference.compareAndSet(previousState, newState)) {
                return true;
            } else {
                previousState = stateReference.get();
                newState.copyState(previousState);
            }
        }
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyTimeLimit) throws InterruptedException {
        boolean isWaitingLimited = waitIfBusyTimeLimit > 0;

        final long methodStartTime = isWaitingLimited? configuration.getTimeMeter().currentTime(): 0;
        long currentTime = methodStartTime;
        long methodDuration = 0;
        boolean isFirstCycle = true;

        BucketLocalState previousState = stateReference.get();
        BucketLocalState newState = previousState.clone();

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

            configuration.getRefillStrategy().refill(configuration, newState, currentTime);
            long availableToConsume = newState.getAvailableTokens(configuration);
            if (tokensToConsume <= availableToConsume) {
                newState.consume(configuration, tokensToConsume);
                if (stateReference.compareAndSet(previousState, newState)) {
                    return true;
                } else {
                    previousState = stateReference.get();
                    newState.copyState(previousState);
                    continue;
                }
            }

            long deficitTokens = tokensToConsume - availableToConsume;
            long timeToCloseDeficit = newState.calculateTimeToCloseDeficit(configuration, deficitTokens);
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
        return stateReference.get().clone();
    }
}