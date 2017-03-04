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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.github.bucket4j.BucketExceptions.nullTimeMeter;

/**
 * A builder for buckets. Builder can be reused, i.e. one builder can create multiple buckets with similar configuration.
 *
 * @see com.github.bucket4j.local.LockFreeBucket
 * @see com.github.bucket4j.grid.GridBucket
 */
public abstract class AbstractBucketBuilder<T extends AbstractBucketBuilder> {

    private TimeMeter timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
    private List<Bandwidth> bandwidths = new ArrayList<>(1);

    /**
     * Adds guaranteed bandwidth for all buckets which will be constructed by this builder instance.
     * <p>
     * Guaranteed bandwidth provides following feature: if tokens can be consumed from guaranteed bandwidth,
     * then bucket4j do not perform checking of any limited bandwidths.
     * <p>
     * Unlike limited bandwidths, you can use only one guaranteed bandwidth per single bucket.
     * <p>
     * Rate(which calculated as {@code maxCapacity/(timeUnit*period)}) of guaranteed bandwidth should be strongly lesser then rate of any limited bandwidth,
     * else you will get {@link java.lang.IllegalArgumentException} during construction of bucket.
     *
     * <pre>
     * {@code
     * // Adds bandwidth which guarantees, that client of bucket will be able to consume 1 tokens per 10 minutes,
     * // regardless of limitations.
     * builder.withGuaranteedBandwidth(1, TimeUnit.MINUTES, 10);
     * }
     * </pre>
     *
     * @param maxCapacity the maximum capacity of bandwidth
     * @param period Period of bandwidth.
     *
     * @return this builder instance
     */
    public T withGuaranteedBandwidth(long maxCapacity, Duration period) {
        return withGuaranteedBandwidth(maxCapacity, maxCapacity, period);
    }

    /**
     * Adds guaranteed bandwidth for all buckets which will be constructed by this builder instance.
     * <p>
     * Guaranteed bandwidth provides following feature: if tokens can be consumed from guaranteed bandwidth,
     * then bucket4j do not perform checking of any limited bandwidths.
     * <p>
     * Unlike limited bandwidths, you can use only one guaranteed bandwidth per single bucket.
     * <p>
     * Rate(which calculated as {@code maxCapacity/(timeUnit*period)}) of guaranteed bandwidth should be strongly lesser then rate of any limited bandwidth,
     * else you will get {@link java.lang.IllegalArgumentException} during construction of bucket.
     *
     * <pre>
     * {@code
     * // Adds bandwidth which guarantees, that client of bucket will be able to consume 1 tokens per 10 minutes,
     * // regardless of limitations. Size of bandwidth is 0 after construction
     * builder.withGuaranteedBandwidth(2, TimeUnit.MINUTES, 1, 0);
     * }
     * </pre>
     *
     * @param maxCapacity the maximum capacity of bandwidth
     * @param initialCapacity initial capacity of bandwidth.
     * @param period Period of bandwidth.
     *
     * @return this builder instance
     */
    public T withGuaranteedBandwidth(long maxCapacity, long initialCapacity, Duration period) {
        Refill refill = Refill.smooth(maxCapacity, period);
        Capacity capacity = Capacity.constant(maxCapacity);
        return withGuaranteedBandwidth(capacity, initialCapacity, refill);
    }

    /**
     * Adds guaranteed bandwidth for all buckets which will be constructed by this builder instance.
     * <p>
     * Guaranteed bandwidth provides following feature: if tokens can be consumed from guaranteed bandwidth,
     * then bucket4j do not perform checking of any limited bandwidths.
     * <p>
     * Unlike limited bandwidths, you can use only one guaranteed bandwidth per single bucket.
     * <p>
     *
     * In opposite to method {@link #withGuaranteedBandwidth(long, Duration)},
     * this method does not perform checking of limitation
     * which disallow to have greater rate of guaranteed than rate of limited bandwidth,
     * because rate is dynamic and depends from <code>bandwidthAdjuster</code>.
     *
     * @param capacity provider of bandwidth capacity
     * @param initialCapacity initial capacity of bandwidth.
     * @param refill refill policy of bandwidth.
     *
     * @return this builder instance
     */
    public T withGuaranteedBandwidth(Capacity capacity, long initialCapacity, Refill refill) {
        final Bandwidth bandwidth = new Bandwidth(capacity, initialCapacity, refill, true);
        bandwidths.add(bandwidth);
        return (T) this;
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     * <p>
     * You can specify as many limited bandwidth as needed, but with following limitation: each limited bandwidth should has unique period,
     * and when period of bandwidth <tt>X</tt> is greater than bandwidth <tt>Y</tt>,
     * then capacity of bandwidth <tt>X</tt> should be greater capacity of bandwidth <tt>Y</tt>,
     * except cases when capacity of bandwidth <tt>X</tt> or <tt>Y</tt> is dynamic(provided by {@link Capacity}).
     *
     * <pre>
     * {@code
     * // Adds bandwidth that restricts to consume not often 1 tokens per 10 minutes,
     * builder.withLimitedBandwidth(1, TimeUnit.MINUTES, 10);
     * }
     * </pre>
     *
     * @param maxCapacity the maximum capacity of bandwidth
     * @param period Period of bandwidth.
     *
     * @return this builder instance
     */
    public T withLimitedBandwidth(long maxCapacity, Duration period) {
        return withLimitedBandwidth(maxCapacity, maxCapacity, period);
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     * <p>
     * You can specify as many limited bandwidth as needed, but with following limitation: each limited bandwidth should has unique period,
     * and when period of bandwidth <tt>X</tt> is greater than bandwidth <tt>Y</tt>,
     * then capacity of bandwidth <tt>X</tt> should be greater capacity of bandwidth <tt>Y</tt>,
     * except cases when capacity of bandwidth <tt>X</tt> or <tt>Y</tt> is dynamic(provided by {@link Capacity}).
     *
     * <pre>
     * {@code
     * // Adds bandwidth that restricts to consume not often 1 tokens per 10 minutes, and initial capacity 0.
     * builder.withLimitedBandwidth(1, TimeUnit.MINUTES, 10, 0);
     * }
     * </pre>
     *
     * @param maxCapacity the maximum capacity of bandwidth
     * @param initialCapacity initial capacity
     * @param period Period of bandwidth.
     *
     * @return this builder instance
     */
    public T withLimitedBandwidth(long maxCapacity, long initialCapacity, Duration period) {
        Refill refill = Refill.smooth(maxCapacity, period);
        Capacity capacity = Capacity.constant(maxCapacity);
        return withLimitedBandwidth(capacity, initialCapacity, refill);
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     * <p>
     * You can specify as many limited bandwidth as needed, but with following limitation: each limited bandwidth should has unique period,
     * and when period of bandwidth <tt>X</tt> is greater than bandwidth <tt>Y</tt>,
     * then capacity of bandwidth <tt>X</tt> should be greater capacity of bandwidth <tt>Y</tt>,
     * except cases when capacity of bandwidth <tt>X</tt> or <tt>Y</tt> is dynamic(provided by {@link Capacity}).
     *
     * @param capacity provider of bandwidth capacity
     * @param initialCapacity initial capacity
     * @param refill policy of bandwidth.
     *
     * @return this builder instance
     *
     */
    public T withLimitedBandwidth(Capacity capacity, long initialCapacity, Refill refill) {
        final Bandwidth bandwidth = new Bandwidth(capacity, initialCapacity, refill, false);
        bandwidths.add(bandwidth);
        return (T) this;
    }

    /**
     * Creates instance of {@link AbstractBucketBuilder} which will create buckets with {@link com.github.bucket4j.TimeMeter#SYSTEM_NANOTIME} as time meter.
     *
     * @return this builder instance
     */
    public T withNanosecondPrecision() {
        this.timeMeter = TimeMeter.SYSTEM_NANOTIME;
        return (T) this;
    }

    /**
     * Creates instance of {@link AbstractBucketBuilder} which will create buckets with {@link com.github.bucket4j.TimeMeter#SYSTEM_MILLISECONDS} as time meter.
     *
     * @return this builder instance
     */
    public T withMillisecondPrecision() {
        this.timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
        return (T) this;
    }

    /**
     * Creates instance of {@link AbstractBucketBuilder} which will create buckets with {@code customTimeMeter} as time meter.
     *
     * @param customTimeMeter object which will measure time.
     *
     * @return this builder instance
     */
    public T withCustomTimePrecision(TimeMeter customTimeMeter) {
        if (customTimeMeter == null) {
            throw nullTimeMeter();
        }
        this.timeMeter = customTimeMeter;
        return (T) this;
    }

    /**
     * @return configuration which used for bucket construction.
     */
    public BucketConfiguration createConfiguration() {
        return new BucketConfiguration(this.bandwidths, timeMeter);
    }

    @Override
    public String toString() {
        return "AbstractBucketBuilder{" +
                "timeMeter=" + timeMeter +
                ", bandwidths=" + bandwidths +
                '}';
    }

}
