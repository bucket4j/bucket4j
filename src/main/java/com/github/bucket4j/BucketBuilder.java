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

import com.github.bucket4j.grid.GridBucket;
import com.github.bucket4j.grid.GridBucketState;
import com.github.bucket4j.grid.GridProxy;
import com.github.bucket4j.grid.coherence.CoherenceProxy;
import com.github.bucket4j.grid.hazelcast.HazelcastProxy;
import com.github.bucket4j.grid.ignite.IgniteProxy;
import com.github.bucket4j.local.LockFreeBucket;
import com.hazelcast.core.IMap;
import com.tangosol.net.NamedCache;
import org.apache.ignite.IgniteCache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.bucket4j.BucketExceptions.nullTimeMetter;

/**
 * A builder for buckets. Builder can be reused, i.e. one builder can create multiple buckets with similar configuration.
 *
 * @see com.github.bucket4j.local.LockFreeBucket
 * @see com.github.bucket4j.grid.GridBucket
 */
public final class BucketBuilder {

    private TimeMeter timeMeter = TimeMeter.SYSTEM_NANOTIME;
    private List<BandwidthDefinition> bandwidths = new ArrayList<>(1);

    /**
     * Creates a builder for buckets
     *
     * @param timeMeter object which will measure time.
     */
    public BucketBuilder(TimeMeter timeMeter) {
        if (timeMeter == null) {
            throw nullTimeMetter();
        }
        this.timeMeter = timeMeter;
    }

    /**
     * Constructs an instance of {@link com.github.bucket4j.local.LockFreeBucket}
     *
     * @return an instance of {@link com.github.bucket4j.local.LockFreeBucket}
     */
    public Bucket build() {
        BucketConfiguration configuration = createConfiguration();
        return new LockFreeBucket(configuration);
    }

    /**
     * Constructs an instance of {@link com.github.bucket4j.grid.GridBucket} which responsible to limit rate inside Hazelcast cluster.
     *
     * @param imap distributed map which will hold bucket inside cluster.
     *             Feel free to store inside single {@code imap} as mush buckets as you need.
     * @param key  for storing bucket inside {@code imap}.
     *             If you plan to store multiple buckets inside single {@code imap}, then each bucket should has own unique {@code key}.
     *
     * @see com.github.bucket4j.grid.hazelcast.HazelcastProxy
     */
    public Bucket buildHazelcast(IMap<Object, GridBucketState> imap, Serializable key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new HazelcastProxy(imap, key));
    }

    /**
     * Constructs an instance of {@link com.github.bucket4j.grid.GridBucket} which responsible to limit rate inside Apache Ignite(GridGain) cluster.
     *
     * @param cache distributed cache which will hold bucket inside cluster.
     *             Feel free to store inside single {@code cache} as mush buckets as you need.
     * @param key  for storing bucket inside {@code cache}.
     *             If you plan to store multiple buckets inside single {@code cache}, then each bucket should has own unique {@code key}.
     *
     * @see com.github.bucket4j.grid.ignite.IgniteProxy
     */
    public Bucket buildIgnite(IgniteCache<Object, GridBucketState> cache, Object key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new IgniteProxy(cache, key));
    }

    /**
     * Constructs an instance of {@link com.github.bucket4j.grid.GridBucket} which responsible to limit rate inside Oracle Coherence cluster.
     *
     * @param cache distributed cache which will hold bucket inside cluster.
     *             Feel free to store inside single {@code cache} as mush buckets as you need.
     * @param key  for storing bucket inside {@code cache}.
     *             If you plan to store multiple buckets inside single {@code cache}, then each bucket should has own unique {@code key}.
     *
     * @see com.github.bucket4j.grid.coherence.CoherenceProxy
     */
    public Bucket buildCoherence(NamedCache cache, Object key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new CoherenceProxy(cache, key));
    }

    /**
     * Build distributed bucket for custom grid which is not supported out of the box.
     *
     * @param gridProxy delegate for accessing to your grid.
     *
     * @see com.github.bucket4j.grid.GridProxy
     */
    public Bucket buildCustomGrid(GridProxy gridProxy) {
        HashMap map;
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, gridProxy);
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
     * <p>
     * <pre>
     * {@code
     * // Adds bandwidth which guarantees, that client of bucket will be able to consume 1 tokens per 10 minutes,
     * // regardless of limitations.
     * builder.withGuaranteedBandwidth(1, TimeUnit.MINUTES, 10);
     * }
     * </pre>
     *
     * @param maxCapacity the maximum capacity of bandwidth
     * @param timeUnit Unit for period.
     * @param period Period of bandwidth.
     *
     */
    public BucketBuilder withGuaranteedBandwidth(long maxCapacity, TimeUnit timeUnit, long period) {
        return withGuaranteedBandwidth(maxCapacity, timeUnit, period, maxCapacity);
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
     * <p>
     * <pre>
     * {@code
     * // Adds bandwidth which guarantees, that client of bucket will be able to consume 1 tokens per 10 minutes,
     * // regardless of limitations. Size of bandwidth is 0 after construction
     * builder.withGuaranteedBandwidth(2, TimeUnit.MINUTES, 1, 0);
     * }
     * </pre>
     *
     * @param maxCapacity the maximum capacity of bandwidth
     * @param timeUnit Unit for period.
     * @param period Period of bandwidth.
     * @param initialCapacity initial capacity of bandwidth.
     *
     */
    public BucketBuilder withGuaranteedBandwidth(long maxCapacity, TimeUnit timeUnit, long period, long initialCapacity) {
        final long bandwidthPeriod = timeMeter.toBandwidthPeriod(timeUnit, period);
        final BandwidthDefinition bandwidth = new BandwidthDefinition(maxCapacity, initialCapacity, bandwidthPeriod, true);
        bandwidths.add(bandwidth);
        return this;
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
     * In opposite to method {@link #withGuaranteedBandwidth(long, TimeUnit, long)},
     * this method does not perform checking of limitation
     * which disallow to have greater rate of guaranteed than rate of limited bandwidth,
     * because rate is dynamic and depends from <code>bandwidthAdjuster</code>.
     *
     * @param bandwidthAdjuster provider of bandwidth capacity
     * @param timeUnit Unit for period.
     * @param period Period of bandwidth.
     * @param initialCapacity initial capacity of bandwidth.
     *
     */
    public BucketBuilder withGuaranteedBandwidth(BandwidthAdjuster bandwidthAdjuster, TimeUnit timeUnit, long period, long initialCapacity) {
        final long bandwidthPeriod = timeMeter.toBandwidthPeriod(timeUnit, period);
        final BandwidthDefinition bandwidth = new BandwidthDefinition(bandwidthAdjuster, initialCapacity, bandwidthPeriod, true);
        bandwidths.add(bandwidth);
        return this;
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     * <p>
     * You can specify as many limited bandwidth as needed, but with following limitation: each limited bandwidth should has unique period,
     * and when period of bandwidth <tt>X</tt> is greater than bandwidth <tt>Y</tt>,
     * then capacity of bandwidth <tt>X</tt> should be greater capacity of bandwidth <tt>Y</tt>,
     * except cases when capacity of bandwidth <tt>X</tt> or <tt>Y</tt> is dynamic(provided by {@link com.github.bucket4j.BandwidthAdjuster}).
     * <p>
     * <pre>
     * {@code
     * // Adds bandwidth that restricts to consume not often 1 tokens per 10 minutes,
     * builder.withLimitedBandwidth(1, TimeUnit.MINUTES, 10);
     * }
     * </pre>
     *
     * @param maxCapacity the maximum capacity of bandwidth
     * @param timeUnit Unit for period.
     * @param period Period of bandwidth.
     *
     */
    public BucketBuilder withLimitedBandwidth(long maxCapacity, TimeUnit timeUnit, long period) {
        return withLimitedBandwidth(maxCapacity, timeUnit, period, maxCapacity);
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     * <p>
     * You can specify as many limited bandwidth as needed, but with following limitation: each limited bandwidth should has unique period,
     * and when period of bandwidth <tt>X</tt> is greater than bandwidth <tt>Y</tt>,
     * then capacity of bandwidth <tt>X</tt> should be greater capacity of bandwidth <tt>Y</tt>,
     * except cases when capacity of bandwidth <tt>X</tt> or <tt>Y</tt> is dynamic(provided by {@link com.github.bucket4j.BandwidthAdjuster}).
     * <p>
     * <pre>
     * {@code
     * // Adds bandwidth that restricts to consume not often 1 tokens per 10 minutes, and initial capacity 0.
     * builder.withLimitedBandwidth(1, TimeUnit.MINUTES, 10, 0);
     * }
     * </pre>
     *
     * @param maxCapacity the maximum capacity of bandwidth
     * @param timeUnit Unit for period.
     * @param period Period of bandwidth.
     * @param initialCapacity initial capacity
     *
     */
    public BucketBuilder withLimitedBandwidth(long maxCapacity, TimeUnit timeUnit, long period, long initialCapacity) {
        final long bandwidthPeriod = timeMeter.toBandwidthPeriod(timeUnit, period);
        final BandwidthDefinition bandwidth = new BandwidthDefinition(maxCapacity, initialCapacity, bandwidthPeriod, false);
        bandwidths.add(bandwidth);
        return this;
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     * <p>
     * You can specify as many limited bandwidth as needed, but with following limitation: each limited bandwidth should has unique period,
     * and when period of bandwidth <tt>X</tt> is greater than bandwidth <tt>Y</tt>,
     * then capacity of bandwidth <tt>X</tt> should be greater capacity of bandwidth <tt>Y</tt>,
     * except cases when capacity of bandwidth <tt>X</tt> or <tt>Y</tt> is dynamic(provided by {@link com.github.bucket4j.BandwidthAdjuster}).
     *
     * @param bandwidthAdjuster provider of bandwidth capacity
     * @param timeUnit Unit for period.
     * @param period Period of bandwidth.
     * @param initialCapacity initial capacity
     *
     */
    public BucketBuilder withLimitedBandwidth(BandwidthAdjuster bandwidthAdjuster, TimeUnit timeUnit, long period, long initialCapacity) {
        final long bandwidthPeriod = timeMeter.toBandwidthPeriod(timeUnit, period);
        final BandwidthDefinition bandwidth = new BandwidthDefinition(bandwidthAdjuster, initialCapacity, bandwidthPeriod, false);
        bandwidths.add(bandwidth);
        return this;
    }

    /**
     * @return Time meter used for time measuring.
     */
    public TimeMeter getTimeMeter() {
        return timeMeter;
    }

    /**
     * @return configuration which used for bucket construction.
     */
    public BucketConfiguration createConfiguration() {
        return new BucketConfiguration(this.bandwidths, timeMeter);
    }

    @Override
    public String toString() {
        return "BucketBuilder{" +
                "timeMeter=" + timeMeter +
                ", bandwidths=" + bandwidths +
                '}';
    }

    BandwidthDefinition getBandwidthDefinition(int index) {
        return bandwidths.get(index);
    }

}
