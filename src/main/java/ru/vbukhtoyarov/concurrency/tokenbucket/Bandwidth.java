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

package ru.vbukhtoyarov.concurrency.tokenbucket;

import java.util.concurrent.TimeUnit;

public class Bandwidth {

    private final long capacity;
    private final long periodInNanos;
    private final RefillStrategy refillStrategy;
    private final WaitingStrategy waitingStrategy;

    /**
     * Creates a Bandwidth.
     *
     * @param capacity
     * @param period
     * @param unit
     * @param refillStrategy
     * @param waitingStrategy
     */
    public Bandwidth(long capacity, long period, TimeUnit unit, RefillStrategy refillStrategy, WaitingStrategy waitingStrategy) {
        this.capacity = capacity;
        this.periodInNanos = unit.toNanos(period);
        this.refillStrategy = refillStrategy;
        this.waitingStrategy = waitingStrategy;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getPeriodInNanos() {
        return periodInNanos;
    }

    public RefillStrategy getRefillStrategy() {
        return refillStrategy;
    }

    public WaitingStrategy getWaitingStrategy() {
        return waitingStrategy;
    }

}