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

package com.github.bandwidthlimiter.tokenbucket;

import java.util.concurrent.TimeUnit;

public final class BandwidthDefinition {

    final long capacity;
    final long initialCapacity;
    final long periodInNanos;
    final RefillStrategy refillStrategy;
    final WaitingStrategy waitingStrategy;
    final double tokensPerNanosecond;
    final double nanosecondsPerToken;

    BandwidthDefinition(long capacity, long initialCapacity, long period, TimeUnit timeUnit, RefillStrategy refillStrategy, WaitingStrategy waitingStrategy) {
        if (capacity <= 0) {
            throw TokenBucketExceptions.nonPositiveCapacity(capacity);
        }
        if (initialCapacity < 0) {
            throw TokenBucketExceptions.nonPositiveInitialCapacity(initialCapacity);
        }
        if (initialCapacity > capacity) {
            throw TokenBucketExceptions.initialCapacityGreaterThanMaxCapacity(initialCapacity, capacity);
        }
        if (refillStrategy == null) {
            throw TokenBucketExceptions.nullRefillStrategy();
        }
        if (waitingStrategy == null) {
            throw TokenBucketExceptions.nullWaitingStrategy();
        }
        if (timeUnit == null) {
            throw TokenBucketExceptions.nullTimeUnit();
        }
        if (period <= 0) {
            throw TokenBucketExceptions.nonPositivePeriod(period);
        }

        this.capacity = capacity;
        this.initialCapacity = initialCapacity;
        this.periodInNanos = timeUnit.toNanos(period);
        this.refillStrategy = refillStrategy;
        this.waitingStrategy = waitingStrategy;
        this.tokensPerNanosecond = (double) capacity / (double) periodInNanos;
        this.nanosecondsPerToken = (double) periodInNanos / (double) capacity;
    }

    public void sleep(long nanosToAwait) throws InterruptedException {
        waitingStrategy.sleep(nanosToAwait);
    }

    public long refill(long previousRefillNanoTime, long currentNanoTime) {
        return refillStrategy.refill(this, previousRefillNanoTime, currentNanoTime);
    }

    public long nanosRequiredToRefill(long numTokens) {
        return refillStrategy.nanosRequiredToRefill(this, numTokens);
    }

    @Override
    public String toString() {
        return "BandwidthDefinition{" +
                "capacity=" + capacity +
                ", initialCapacity=" + initialCapacity +
                ", periodInNanos=" + periodInNanos +
                ", refillStrategy=" + refillStrategy +
                ", waitingStrategy=" + waitingStrategy +
                ", tokensPerNanosecond=" + tokensPerNanosecond +
                ", nanosecondsPerToken=" + nanosecondsPerToken +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BandwidthDefinition that = (BandwidthDefinition) o;

        if (capacity != that.capacity) return false;
        if (initialCapacity != that.initialCapacity) return false;
        if (periodInNanos != that.periodInNanos) return false;
        if (refillStrategy != null ? !refillStrategy.equals(that.refillStrategy) : that.refillStrategy != null)
            return false;
        if (waitingStrategy != null ? !waitingStrategy.equals(that.waitingStrategy) : that.waitingStrategy != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (capacity ^ (capacity >>> 32));
        result = 31 * result + (int) (initialCapacity ^ (initialCapacity >>> 32));
        result = 31 * result + (int) (periodInNanos ^ (periodInNanos >>> 32));
        result = 31 * result + (refillStrategy != null ? refillStrategy.hashCode() : 0);
        result = 31 * result + (waitingStrategy != null ? waitingStrategy.hashCode() : 0);
        return result;
    }

}