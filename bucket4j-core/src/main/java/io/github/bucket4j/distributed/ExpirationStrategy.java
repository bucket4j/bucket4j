package io.github.bucket4j.distributed;

import io.github.bucket4j.distributed.remote.RemoteBucketState;

import java.time.Duration;

/**
 * Represents the strategy for choosing time to live for buckets in the cache.
 */
public interface ExpirationStrategy {

    /**
     * Calculates the time to live for bucket that is going to be persisted to the remote storage
     *
     * @param state the state of bucket that is going to be persisted to the remote storage
     * @param currentTimeNanos the time of operation
     *
     * @return time to live for bucket in milliseconds, negative value must be considered that bucket should be stored forever without expiration
     */
    long calculateTimeToLiveMillis(RemoteBucketState state, long currentTimeNanos);

    /**
     * Returns strategy that makes to store buckets forever.
     * <p>Do not use this strategy in use-case of many buckets in order to avoid storage overflowing by unused buckets.
     *
     * @return
     *
     * @see #fixed(Duration)
     * @see #basedOnTimeForRefillingBucketUpToMax(Duration)
     */
    static ExpirationStrategy none() {
        return (state, currentTimeNanos)  -> -1;
    }

    /**
     * Returns strategy that makes to store buckets with fixed TTL.
     * <p>
     * This strategy can lead to overconsumption if provided ttl is less significantly than refill period,
     * because information about previously consumed in such misconfiguration case can be lost in remote storage faster than it should be.
     * As well as consumed capacity to store all buckets in storage can be significantly more than it can be in case of TTL is significantly greater than refill period of bucket.
     * So consider to use {@link #basedOnTimeForRefillingBucketUpToMax(Duration)} for more precise TTL calculation based on current bucket state.
     *
     * @param ttl fixed TTL that will be used for storing the buckets
     *
     * @return
     * @see #basedOnTimeForRefillingBucketUpToMax(Duration)
     */
    static ExpirationStrategy fixed(Duration ttl) {
        long ttlMillis = ttl.toMillis();
        return (state, currentTimeNanos) -> ttlMillis;
    }

    /**
     * Returns strategy that smartly calculates TTL based of current bucket state,
     * buckets are not persisted in storage longer than time required to refill all consumed tokens plus <b>keepAfterRefillDuration</b>
     *
     * @param keepAfterRefillDuration specifies how long bucket should be held in the cache after all consumed tokens have been refilled.
     *                                It can be a zero duration, but from performance considerations it is recommended to provide at least few seconds,
     *                                because reusing of already persisted can be faster that initialization of bucket from scratch,
     *                                so <b>keepAfterRefillDuration</b> helps to not evict buckets from storage too frequent.
     * @return
     *
     * @see #basedOnTimeForRefillingBucketUpToMax(Duration)
     */
    static ExpirationStrategy basedOnTimeForRefillingBucketUpToMax(Duration keepAfterRefillDuration) {
        long keepAfterRefillDurationMillis = keepAfterRefillDuration.toMillis();
        return (state, currentTimeNanos) -> {
            long millisToFullRefill = state.calculateFullRefillingTime(currentTimeNanos) / 1_000_000;
            return keepAfterRefillDurationMillis + millisToFullRefill;
        };
    }

}
