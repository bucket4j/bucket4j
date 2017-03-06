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
    private final Refill refill;

    private Bandwidth(Capacity capacity, Refill refill) {
        this.capacity = capacity;
        this.refill = refill;
    }

    public static Bandwidth simple(long capacity, Duration period) {
        Refill refill = Refill.smooth(capacity, period);
        return new Bandwidth(Capacity.constant(capacity), refill);
    }

    public static Bandwidth classic(long maxCapacity, Refill refill) {
        return new Bandwidth(Capacity.constant(maxCapacity), refill);
    }

    public static Bandwidth classic(Capacity capacity, Refill refill) {
        return new Bandwidth(capacity, refill);
    }

    public Refill getRefill() {
        return refill;
    }

    public Capacity getCapacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return "Bandwidth{" +
                "capacity=" + capacity +
                ", refill=" + refill +
                '}';
    }

}