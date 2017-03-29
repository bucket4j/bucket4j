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
package com.github.bucket4j.local;


import com.github.bucket4j.AbstractBucket;
import com.github.bucket4j.Bandwidth;
import com.github.bucket4j.BucketConfiguration;
import com.github.bucket4j.BucketState;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeBucket extends AbstractBucket {

    private final AtomicReference<BucketState> stateReference;
    private final BucketConfiguration configuration;

    public LockFreeBucket(BucketConfiguration configuration) {
        super(configuration);
        this.configuration = configuration;
        BucketState initialState = BucketState.createInitialState(configuration);
        this.stateReference = new AtomicReference<>(initialState);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        BucketState previousState = stateReference.get();
        BucketState newState = previousState.clone();
        Bandwidth[] limits = configuration.getBandwidths();
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(limits, currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens(limits);
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume == 0) {
                return 0;
            }
            newState.consume(limits, toConsume);
            if (stateReference.compareAndSet(previousState, newState)) {
                return toConsume;
            } else {
                previousState = stateReference.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        BucketState previousState = stateReference.get();
        BucketState newState = previousState.clone();
        Bandwidth[] limits = configuration.getBandwidths();
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(limits, currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens(limits);
            if (tokensToConsume > availableToConsume) {
                return false;
            }
            newState.consume(limits, tokensToConsume);
            if (stateReference.compareAndSet(previousState, newState)) {
                return true;
            } else {
                previousState = stateReference.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyTimeLimit) throws InterruptedException {
        Bandwidth[] limits = configuration.getBandwidths();
        boolean isWaitingLimited = waitIfBusyTimeLimit > 0;

        final long methodStartTimeNanos = configuration.getTimeMeter().currentTimeNanos();
        long currentTimeNanos = methodStartTimeNanos;
        long methodDuration = 0;
        boolean isFirstCycle = true;

        BucketState previousState = stateReference.get();
        BucketState newState = previousState.clone();

        while (true) {
            if (isFirstCycle) {
                isFirstCycle = false;
            } else {
                currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();
                methodDuration = currentTimeNanos - methodStartTimeNanos;
                if (isWaitingLimited && methodDuration >= waitIfBusyTimeLimit) {
                    return false;
                }
                previousState = stateReference.get();
                newState.copyStateFrom(previousState);
            }

            newState.refillAllBandwidth(limits, currentTimeNanos);
            long nanosToCloseDeficit = newState.delayNanosAfterWillBePossibleToConsume(limits, currentTimeNanos, tokensToConsume);
            if (nanosToCloseDeficit == Long.MAX_VALUE) {
                throw new IllegalArgumentException("tokensToConsume should be <= capacity");
            }
            if (nanosToCloseDeficit == 0) {
                newState.consume(limits, tokensToConsume);
                if (stateReference.compareAndSet(previousState, newState)) {
                    return true;
                } else {
                    continue;
                }
            }

            if (isWaitingLimited) {
                long sleepingTimeLimit = waitIfBusyTimeLimit - methodDuration;
                if (nanosToCloseDeficit >= sleepingTimeLimit) {
                    return false;
                }
            }
            configuration.getTimeMeter().parkNanos(nanosToCloseDeficit);
        }
    }

    @Override
    protected void addTokensIml(long tokensToAdd) {
        BucketState previousState = stateReference.get();
        BucketState newState = previousState.clone();
        Bandwidth[] limits = configuration.getBandwidths();
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(limits, currentTimeNanos);
            newState.addTokens(limits, tokensToAdd, currentTimeNanos);
            if (stateReference.compareAndSet(previousState, newState)) {
                return;
            } else {
                previousState = stateReference.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    public BucketState createSnapshot() {
        return stateReference.get().clone();
    }

    @Override
    public String toString() {
        return "LockFreeBucket{" +
                "state=" + stateReference.get() +
                ", configuration=" + configuration +
                '}';
    }

}