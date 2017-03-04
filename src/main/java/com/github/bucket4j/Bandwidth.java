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

public class Bandwidth implements Serializable {

    private final Capacity capacity;
    private final long initialTokens;
    private final Refill refill;
    private final boolean guaranteed;

    public Bandwidth(Capacity capacity, long initialTokens, Refill refill, boolean guaranteed) {
        this.capacity = capacity;
        this.initialTokens = initialTokens;
        this.refill = refill;
        this.guaranteed = guaranteed;
    }

    public boolean isGuaranteed() {
        return guaranteed;
    }

    public boolean isLimited() {
        return !guaranteed;
    }

    public Refill getRefill() {
        return refill;
    }

    public Capacity getCapacity() {
        return capacity;
    }

    public long getInitialTokens() {
        return initialTokens;
    }

    @Override
    public String toString() {
        return "Bandwidth{" +
                "capacity=" + capacity +
                ", initialTokens=" + initialTokens +
                ", refill=" + refill +
                ", guaranteed=" + guaranteed +
                '}';
    }

}