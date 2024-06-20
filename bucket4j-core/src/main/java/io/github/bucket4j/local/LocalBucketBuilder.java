/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

package io.github.bucket4j.local;

import io.github.bucket4j.*;
import io.github.bucket4j.BandwidthBuilder.BandwidthBuilderBuildStage;
import io.github.bucket4j.BandwidthBuilder.BandwidthBuilderCapacityStage;

import java.util.Objects;
import java.util.function.Function;

/**
 * This builder creates in-memory buckets ({@link LockFreeBucket}).
 */
public class LocalBucketBuilder {

    private final ConfigurationBuilder configurationBuilder;

    public LocalBucketBuilder() {
        configurationBuilder = new ConfigurationBuilder();
    }

    /**
     * Adds limited bandwidth for all buckets which will be constructed by this builder.
     *
     * @param bandwidth limitation
     * @return this builder instance
     */
    public LocalBucketBuilder addLimit(Bandwidth bandwidth) {
        configurationBuilder.addLimit(bandwidth);
        return this;
    }

    public LocalBucketBuilder addLimit(Function<BandwidthBuilderCapacityStage, BandwidthBuilderBuildStage> bandwidthConfigurator) {
        if (bandwidthConfigurator == null) {
            throw BucketExceptions.nullBuilder();
        }
        BandwidthBuilderBuildStage builder = bandwidthConfigurator.apply(Bandwidth.builder());
        Bandwidth bandwidth = builder.build();
        return addLimit(bandwidth);
    }

    private TimeMeter timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
    private ConcurrencyStrategy concurrencyStrategy = ConcurrencyStrategy.LOCK_FREE;
    private BucketListener listener = BucketListener.NOPE;

    /**
     * Specifies {@link TimeMeter#SYSTEM_NANOTIME} as time meter for buckets that will be created by this builder.
     *
     * @return this builder instance
     */
    public LocalBucketBuilder withNanosecondPrecision() {
        this.timeMeter = TimeMeter.SYSTEM_NANOTIME;
        return this;
    }

    /**
     * Specifies {@link TimeMeter#SYSTEM_MILLISECONDS} as time meter for buckets that will be created by this builder.
     *
     * @return this builder instance
     */
    public LocalBucketBuilder withMillisecondPrecision() {
        this.timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
        return this;
    }

    /**
     * Specifies {@code customTimeMeter} time meter for buckets that will be created by this builder.
     *
     * @param customTimeMeter object which will measure time.
     *
     * @return this builder instance
     */
    public LocalBucketBuilder withCustomTimePrecision(TimeMeter customTimeMeter) {
        if (customTimeMeter == null) {
            throw BucketExceptions.nullTimeMeter();
        }
        this.timeMeter = customTimeMeter;
        return this;
    }

    /**
     * Specifies {@code synchronizationStrategy} for buckets that will be created by this builder.
     *
     * @param concurrencyStrategy the strategy of synchronization which need to be applied to prevent data-races in multi-threading usage scenario.
     *
     * @return this builder instance
     */
    public LocalBucketBuilder withSynchronizationStrategy(ConcurrencyStrategy concurrencyStrategy) {
        if (concurrencyStrategy == null) {
            throw BucketExceptions.nullSynchronizationStrategy();
        }
        this.concurrencyStrategy = concurrencyStrategy;
        return this;
    }

    /**
     * Specifies {@code listener} for buckets that will be created by this builder.
     *
     * @param listener the listener of bucket events.
     *
     * @return this builder instance
     */
    public LocalBucketBuilder withListener(BucketListener listener) {
        this.listener = Objects.requireNonNull(listener);
        return this;
    }

    /**
     * Constructs the bucket.
     *
     * @return the new bucket
     */
    public LocalBucket build() {
        BucketConfiguration configuration = buildConfiguration();
        return concurrencyStrategy.createBucket(configuration, MathType.INTEGER_64_BITS, timeMeter, listener);
    }

    private BucketConfiguration buildConfiguration() {
        BucketConfiguration configuration = configurationBuilder.build();
        for (Bandwidth bandwidth : configuration.getBandwidths()) {
            if (bandwidth.isIntervallyAligned() && !timeMeter.isWallClockBased()) {
                throw BucketExceptions.intervallyAlignedRefillCompatibleOnlyWithWallClock();
            }
        }
        return configuration;
    }

}
