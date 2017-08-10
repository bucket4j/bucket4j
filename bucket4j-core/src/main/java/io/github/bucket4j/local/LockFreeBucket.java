/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.local;


import io.github.bucket4j.*;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeBucket extends AbstractBucket {

    private final BucketConfiguration configuration;
    private final Bandwidth[] bandwidths;
    private final TimeMeter timeMeter;
    private final AtomicReference<BucketState> stateReference;

    public LockFreeBucket(BucketConfiguration configuration) {
        super(true);
        this.configuration = configuration;
        this.bandwidths = configuration.getBandwidths();
        this.timeMeter = configuration.getTimeMeter();
        BucketState initialState = BucketState.createInitialState(configuration);
        this.stateReference = new AtomicReference<>(initialState);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        BucketState previousState = stateReference.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens(bandwidths);
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume == 0) {
                return 0;
            }
            newState.consume(bandwidths, toConsume);
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
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens(bandwidths);
            if (tokensToConsume > availableToConsume) {
                return false;
            }
            newState.consume(bandwidths, tokensToConsume);
            if (stateReference.compareAndSet(previousState, newState)) {
                return true;
            } else {
                previousState = stateReference.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume) {
        BucketState previousState = stateReference.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = newState.getAvailableTokens(bandwidths);
            if (tokensToConsume > availableToConsume) {
                long nanosToWaitForRefill = newState.delayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume);
                return ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill);
            }
            newState.consume(bandwidths, tokensToConsume);
            if (stateReference.compareAndSet(previousState, newState)) {
                return ConsumptionProbe.consumed(availableToConsume - tokensToConsume);
            } else {
                previousState = stateReference.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyNanosLimit, boolean uninterruptibly, BlockingStrategy blockingStrategy) throws InterruptedException {
        BucketState previousState = stateReference.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(bandwidths, currentTimeNanos);
            long nanosToCloseDeficit = newState.delayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume);
            if (nanosToCloseDeficit == 0) {
                newState.consume(bandwidths, tokensToConsume);
                if (stateReference.compareAndSet(previousState, newState)) {
                    return true;
                }
                previousState = stateReference.get();
                newState.copyStateFrom(previousState);
                continue;
            }

            if (waitIfBusyNanosLimit > 0 && nanosToCloseDeficit > waitIfBusyNanosLimit) {
                return false;
            }

            newState.consume(bandwidths, tokensToConsume);
            if (stateReference.compareAndSet(previousState, newState)) {
                park(blockingStrategy, nanosToCloseDeficit, uninterruptibly);
                return true;
            }
            previousState = stateReference.get();
            newState.copyStateFrom(previousState);
        }
    }

    private void park(BlockingStrategy blockingStrategy, long nanosToCloseDeficit, boolean uninterruptibly) throws InterruptedException {
        if (uninterruptibly) {
            blockingStrategy.parkUninterruptibly(nanosToCloseDeficit);
        } else {
            blockingStrategy.park(nanosToCloseDeficit);
        }
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        BucketState previousState = stateReference.get();
        BucketState newState = previousState.copy();
        long currentTimeNanos = timeMeter.currentTimeNanos();

        while (true) {
            newState.refillAllBandwidth(bandwidths, currentTimeNanos);
            newState.addTokens(bandwidths, tokensToAdd);
            if (stateReference.compareAndSet(previousState, newState)) {
                return;
            } else {
                previousState = stateReference.get();
                newState.copyStateFrom(previousState);
            }
        }
    }

    @Override
    public long getAvailableTokens() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        BucketState snapshot = stateReference.get().copy();
        snapshot.refillAllBandwidth(bandwidths, currentTimeNanos);
        return snapshot.getAvailableTokens(bandwidths);
    }

    @Override
    public BucketState createSnapshot() {
        return stateReference.get().copy();
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String toString() {
        return "LockFreeBucket{" +
                "state=" + stateReference.get() +
                ", configuration=" + getConfiguration() +
                '}';
    }

}