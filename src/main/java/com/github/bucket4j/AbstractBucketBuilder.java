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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.github.bucket4j.BucketExceptions.nullTimeMeter;

/**
 * A builder for buckets. Builder can be reused, i.e. one builder can create multiple buckets with similar configuration.
 *
 * @see Bucket
 * @see com.github.bucket4j.local.LocalBucketBuilder
 * @see com.github.bucket4j.grid.jcache.JCacheBucketBuilder
 */
public abstract class AbstractBucketBuilder<T extends AbstractBucketBuilder> {

    private TimeMeter timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
    private List<Bandwidth> limitedBandwidths = new ArrayList<>(1);
    private Bandwidth guaranteedBandwidth;

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder instance.
     *
     *
     * @param bandwidth limitation
     * @return this builder instance
     */
    public T addLimit(Bandwidth bandwidth) {
        Objects.requireNonNull(bandwidth);
        limitedBandwidths.add(bandwidth);
        return (T) this;
    }

    /**
     * Specifies guaranteed bandwidth for all buckets which will be constructed by this builder instance.
     *
     * <p>
     * Guaranteed bandwidth provides following feature - if tokens can be consumed from guaranteed bandwidth,
     * then bucket does not check of any limited bandwidths.
     * <pre>{@code // Adds bandwidth which guarantees, that client of bucket will be able to consume 1 tokens per 10 minutes, regardless of limitations.
     * builder.setGuarantee(Bandwidth.create(1, Duration.ofMinutes(10)));
     * }</pre>
     *
     * <p> Only one guaranteed bandwidth can be specified for bucket, if guaranteed bandwidth already specified,
     * then previous bandwidth will be discarded.
     *
     * @param bandwidth guarantee
     * @return this builder instance
     */
    public T setGuarantee(Bandwidth bandwidth) {
        Objects.requireNonNull(bandwidth);
        guaranteedBandwidth = bandwidth;
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
        return new BucketConfiguration(this.limitedBandwidths, guaranteedBandwidth, timeMeter);
    }

    @Override
    public String toString() {
        return "AbstractBucketBuilder{" +
                "timeMeter=" + timeMeter +
                ", limitedBandwidths=" + limitedBandwidths +
                ", guaranteedBandwidth=" + guaranteedBandwidth +
                '}';
    }
}
