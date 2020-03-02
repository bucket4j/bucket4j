
package io.github.bucket4j.local;

import io.github.bucket4j.*;

/**
 * This builder creates in-memory buckets ({@link LockFreeBucket}).
 */
public class LocalBucketBuilder extends AbstractBucketBuilder<LocalBucketBuilder> {

    private TimeMeter timeMeter = TimeMeter.SYSTEM_MILLISECONDS;
    private SynchronizationStrategy synchronizationStrategy = SynchronizationStrategy.LOCK_FREE;

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

        for (Bandwidth bandwidth : configuration.getBandwidths()) {
            if (bandwidth.isIntervallyAligned() && !timeMeter.isWallClockBased()) {
                throw BucketExceptions.intervallyAlignedRefillCompatibleOnlyWithWallClock();
            }
        }

        switch (synchronizationStrategy) {
            case LOCK_FREE: return new LockFreeBucket(configuration, timeMeter);
            case SYNCHRONIZED: return new SynchronizedBucket(configuration, timeMeter);
            case NONE: return new SynchronizedBucket(configuration, timeMeter, FakeLock.INSTANCE);
            default: throw new IllegalStateException();
        }
    }

}
