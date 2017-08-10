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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronizedBucket extends AbstractBucket {

    private final BucketConfiguration configuration;
    private final Bandwidth[] bandwidths;
    private final TimeMeter timeMeter;
    private final BucketState state;
    private final Lock lock;

    public SynchronizedBucket(BucketConfiguration configuration, TimeMeter timeMeter) {
        this(configuration, timeMeter, new ReentrantLock());
    }

    public SynchronizedBucket(BucketConfiguration configuration, TimeMeter timeMeter, Lock lock) {
        this.configuration = configuration;
        this.bandwidths = configuration.getBandwidths();
        this.timeMeter = timeMeter;
        this.state = BucketState.createInitialState(configuration, timeMeter.currentTimeNanos());
        this.lock = lock;
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            long toConsume = Math.min(limit, availableToConsume);
            if (toConsume == 0) {
                return 0;
            }
            state.consume(bandwidths, toConsume);
            return toConsume;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            if (tokensToConsume > availableToConsume) {
                return false;
            }
            state.consume(bandwidths, tokensToConsume);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            long availableToConsume = state.getAvailableTokens(bandwidths);
            if (tokensToConsume > availableToConsume) {
                long nanosToWaitForRefill = state.delayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume);
                return ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill);
            }
            state.consume(bandwidths, tokensToConsume);
            return ConsumptionProbe.consumed(availableToConsume - tokensToConsume);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyNanosLimit, boolean uninterruptibly, BlockingStrategy blockingStrategy) throws InterruptedException {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        long nanosToCloseDeficit;

        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            nanosToCloseDeficit = state.delayNanosAfterWillBePossibleToConsume(bandwidths, tokensToConsume);
            if (nanosToCloseDeficit == 0) {
                state.consume(bandwidths, tokensToConsume);
                return true;
            }

            if (waitIfBusyNanosLimit > 0 && nanosToCloseDeficit > waitIfBusyNanosLimit) {
                return false;
            }

            state.consume(bandwidths, tokensToConsume);
        } finally {
            lock.unlock();
        }
        if (uninterruptibly) {
            blockingStrategy.parkUninterruptibly(nanosToCloseDeficit);
        } else {
            blockingStrategy.park(nanosToCloseDeficit);
        }
        return true;
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            state.addTokens(bandwidths, tokensToAdd);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getAvailableTokens() {
        long currentTimeNanos = timeMeter.currentTimeNanos();
        lock.lock();
        try {
            state.refillAllBandwidth(bandwidths, currentTimeNanos);
            return state.getAvailableTokens(bandwidths);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BucketState createSnapshot() {
        lock.lock();
        try {
            return state.copy();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String toString() {
        synchronized (this) {
            return "SynchronizedBucket{" +
                "state=" + state +
                ", configuration=" + getConfiguration() +
                '}';
        }
    }

}