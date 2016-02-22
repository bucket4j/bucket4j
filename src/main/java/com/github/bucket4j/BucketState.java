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

package com.github.bucket4j;

import java.io.Serializable;
import java.util.Arrays;

public class BucketState implements Serializable {

    private long lastRefillTimeNanos;
    private BandwidthState[] bandwidthStates;

    private BucketState(long lastRefillTimeNanos, BandwidthState[] bandwidthStates) {
        this.lastRefillTimeNanos = lastRefillTimeNanos;
        this.bandwidthStates = bandwidthStates;
    }

    @Override
    public BucketState clone() {
        BandwidthState[] bandwidthStatesClones = new BandwidthState[bandwidthStates.length];
        for (int i = 0; i < bandwidthStates.length; i++) {
            bandwidthStatesClones[i] = bandwidthStates[i].clone();
        }
        return new BucketState(lastRefillTimeNanos, bandwidthStatesClones);
    }

    public BandwidthState getBandwidthState(int index) {
        return bandwidthStates[index];
    }

    public long getLastRefillTimeNanos() {
        return lastRefillTimeNanos;
    }

    public void copyStateFrom(BucketState sourceState) {
        lastRefillTimeNanos = sourceState.lastRefillTimeNanos;
        for (int i = 0; i < bandwidthStates.length; i++) {
            bandwidthStates[i].copyStateFrom(sourceState.bandwidthStates[i]);
        }
    }

    public static BucketState createInitialState(BucketConfiguration configuration) {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        BandwidthState[] bandwidthStates = new BandwidthState[bandwidths.length];
        for(int i = 0; i < bandwidthStates.length; i++) {
            bandwidthStates[i] = bandwidths[i].createInitialState();
        }
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();
        return new BucketState(currentTimeNanos, bandwidthStates);
    }

    public long getAvailableTokens(Bandwidth[] bandwidths) {
        long availableByLimitation = Long.MAX_VALUE;
        long availableByGuarantee = 0;
        for (int i = 0; i < bandwidthStates.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            BandwidthState bandwidthState = bandwidthStates[i];
            if (bandwidth.isLimited()) {
                availableByLimitation = Math.min(availableByLimitation, bandwidthState.getCurrentSize());
            } else {
                availableByGuarantee = bandwidthState.getCurrentSize();
            }
        }
        return Math.max(availableByLimitation, availableByGuarantee);
    }

    public void consume(Bandwidth[] bandwidths, long toConsume) {
        for (int i = 0; i < bandwidthStates.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            BandwidthState bandwidthState = bandwidthStates[i];
            bandwidth.consume(bandwidthState, toConsume);
        }
    }

    public long delayNanosAfterWillBePossibleToConsume(Bandwidth[] bandwidths, long currentTime, long tokensToConsume) {
        long delayAfterWillBePossibleToConsumeLimited = 0;
        long delayAfterWillBePossibleToConsumeGuaranteed = Long.MAX_VALUE;
        for (int i = 0; i < bandwidthStates.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            BandwidthState bandwidthState = bandwidthStates[i];
            long delay = bandwidth.delayNanosAfterWillBePossibleToConsume(bandwidthState, currentTime, tokensToConsume);
            if (bandwidth.isGuaranteed()) {
                if (delay == 0) {
                    return 0;
                } else {
                    delayAfterWillBePossibleToConsumeGuaranteed = delay;
                }
                continue;
            }
            if (delay > delayAfterWillBePossibleToConsumeLimited) {
                delayAfterWillBePossibleToConsumeLimited = delay;
            }
        }
        return Math.min(delayAfterWillBePossibleToConsumeLimited, delayAfterWillBePossibleToConsumeGuaranteed);
    }

    public void refill(Bandwidth[] bandwidths, long currentTimeNanos) {
        if (lastRefillTimeNanos == currentTimeNanos) {
            return;
        }
        for (int i = 0; i < bandwidthStates.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            BandwidthState bandwidthState = bandwidthStates[i];
            bandwidth.refill(bandwidthState, lastRefillTimeNanos, currentTimeNanos);
        }
        lastRefillTimeNanos = currentTimeNanos;
    }

    @Override
    public String toString() {
        return "BucketState{" +
                "lastRefillTimeNanos=" + lastRefillTimeNanos +
                ", bandwidthStates=" + Arrays.toString(bandwidthStates) +
                '}';
    }

}
