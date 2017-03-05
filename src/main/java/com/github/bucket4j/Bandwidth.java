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
import java.time.Duration;
import java.util.Optional;

/**
 * The core term of Bucket4j limiting algorithm is bandwidth.
 * Bucket allow to consume any token only if rate of consumption satisfies the limits specified by each configured bandwidth.
 * The bandwidth consists from {@link Capacity capacity} and {@link Refill refill rate}.
 *
 * <p>
 * There are two types different of bandwidth:
 * <ul>
 *     <li><b>Limited:</b> specifies the limitation.
 * <pre>{@code // Adds bandwidth that restricts to consume not often 1000 tokens per 1 minute
 * builder.addLimit(Bandwidth.create(1000, Duration.ofMinutes(1)));
 * }</pre>
 *     </li>
 *     <li><b>Guaranteed:</b> this bandwidth provides following feature - if tokens can be consumed from guaranteed bandwidth,
 * then bucket does not check of any limited bandwidths. Only one guaranteed bandwidth can be specified for bucket:
 * <pre>{@code // Adds bandwidth which guarantees, that client of bucket will be able to consume 1 tokens per 10 minutes, regardless of limitations.
 * builder.withGuaranteedBandwidth(1, TimeUnit.MINUTES, 10);
 * }</pre>
 * </li>
 * </ul>
 *
 * Most likely you will use only one bandwidth per bucket,
 * but in general it is possible to specify more than one bandwidth per bucket,
 * and bucket will handle all bandwidth in strongly atomic way.
 * Strongly atomic means that token will be consumed from all bandwidth or from nothing,
 * in other words any token can not be partially consumed.
 */
public class Bandwidth implements Serializable {

    private final Capacity capacity;
    private final Long initialTokens;
    private final Refill refill;

    private Bandwidth(Capacity capacity, Long initialTokens, Refill refill) {
        this.capacity = capacity;
        this.initialTokens = initialTokens;
        this.refill = refill;
    }

    public static Bandwidth create(long maxCapacity, Duration period) {
        Long initialCapacity = maxCapacity;
        Capacity capacity = Capacity.constant(maxCapacity);
        Refill refill = Refill.smooth(maxCapacity, period);
        return new Bandwidth(capacity, initialCapacity, refill);
    }

    public static Bandwidth create(long maxCapacity, long initialCapacity, Duration period) {
        Capacity capacity = Capacity.constant(maxCapacity);
        Refill refill = Refill.smooth(maxCapacity, period);
        return new Bandwidth(capacity, initialCapacity, refill);
    }

    public static Bandwidth create(long maxCapacity, Refill refill) {
        Long initialCapacity = maxCapacity;
        Capacity capacity = Capacity.constant(maxCapacity);
        return new Bandwidth(capacity, initialCapacity, refill);
    }

    public static Bandwidth create(Capacity capacity, long initialTokens, Refill refill) {
        return new Bandwidth(capacity, initialTokens, refill);
    }

    public Refill getRefill() {
        return refill;
    }

    public Capacity getCapacity() {
        return capacity;
    }

    public Optional<Long> getInitialTokens() {
        return Optional.ofNullable(initialTokens);
    }

    @Override
    public String toString() {
        return "Bandwidth{" +
                "capacity=" + capacity +
                ", initialTokens=" + initialTokens +
                ", refill=" + refill +
                '}';
    }

}