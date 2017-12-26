/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.local;

import io.github.bucket4j.*;

/**
 * This builder creates in-memory buckets ({@link LockFreeBucket}).
 */
public class LocalBucketBuilder extends ConfigurationBuilder<LocalBucketBuilder> {

    private TimeMeter timeMeter;

    public LocalBucketBuilder() {
        this.timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
    }

    /**
     * Creates instance of {@link ConfigurationBuilder} which will create buckets with {@link TimeMeter#SYSTEM_NANOTIME} as time meter.
     *
     * @return this builder instance
     */
    public LocalBucketBuilder withNanosecondPrecision() {
        this.timeMeter = TimeMeter.SYSTEM_NANOTIME;
        return this;
    }

    /**
     * Creates instance of {@link ConfigurationBuilder} which will create buckets with {@link TimeMeter#SYSTEM_MILLISECONDS} as time meter.
     *
     * @return this builder instance
     */
    public LocalBucketBuilder withMillisecondPrecision() {
        this.timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
        return this;
    }

    /**
     * Creates instance of {@link ConfigurationBuilder} which will create buckets with {@code customTimeMeter} as time meter.
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
     * Constructs the bucket using {@link SynchronizationStrategy#LOCK_FREE} synchronization strategy.
     *
     * @return the new bucket
     */
    public LocalBucket build() {
        return build(SynchronizationStrategy.LOCK_FREE);
    }

    /**
     * Constructs the new instance of local bucket which concrete type depends on synchronizationStrategy
     *
     * @param synchronizationStrategy the strategy of synchronization which need to be applied to prevent data-races in multithreading usage scenario.
     *
     * @return the new bucket
     */
    public LocalBucket build(SynchronizationStrategy synchronizationStrategy) {
        BucketConfiguration configuration = buildConfiguration();
        switch (synchronizationStrategy) {
            case LOCK_FREE: return new LockFreeBucket(configuration, timeMeter);
            case SYNCHRONIZED: return new SynchronizedBucket(configuration, timeMeter);
            case NONE: return new SynchronizedBucket(configuration, timeMeter, FakeLock.INSTANCE);
            default: throw new IllegalStateException();
        }
    }

}
