/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.local;

import io.github.bucket4j.*;

/**
 * This builder creates in-memory buckets ({@link LockFreeBucket}).
 */
public class LocalBucketBuilder extends AbstractBucketBuilder<LocalBucketBuilder> {

    public static final BucketOptions OPTIONS = new BucketOptions(true, MathType.ALL, MathType.INTEGER_64_BITS);

    private TimeMeter timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
    private SynchronizationStrategy synchronizationStrategy = SynchronizationStrategy.LOCK_FREE;

    public LocalBucketBuilder() {
        super(OPTIONS);
    }

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
     * @param synchronizationStrategy the strategy of synchronization which need to be applied to prevent data-races in multi-threading usage scenario.
     *
     * @return this builder instance
     */
    public LocalBucketBuilder withSynchronizationStrategy(SynchronizationStrategy synchronizationStrategy) {
        if (synchronizationStrategy == null) {
            throw BucketExceptions.nullSynchronizationStrategy();
        }
        this.synchronizationStrategy = synchronizationStrategy;
        return this;
    }

    /**
     * Constructs the bucket.
     *
     * @return the new bucket
     */
    public LocalBucket build() {
        BucketConfiguration configuration = buildConfiguration();
        switch (synchronizationStrategy) {
            case LOCK_FREE: return new LockFreeBucket(configuration, timeMeter);
            case SYNCHRONIZED: return new SynchronizedBucket(configuration, timeMeter);
            case NONE: return new SynchronizedBucket(configuration, timeMeter, FakeLock.INSTANCE);
            default: throw new IllegalStateException();
        }
    }

}
