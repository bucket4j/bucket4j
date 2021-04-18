package io.github.bucket4j.distributed;

import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;

import java.time.Duration;

/**
 * The optimization controller for {@link BucketProxy}.
 *
 * <p>
 * This interface is actual only if an optimization was applied during bucket construction via {@link RemoteBucketBuilder#withOptimization(Optimization)}
 * otherwise all methods of controller will do nothing.
 */
public interface OptimizationController {

    /**
     * Initiates immediate synchronization of local copy of bucket with remote storage
     */
    default void syncImmediately() {
        syncByCondition(0L, Duration.ZERO);
    }

    /**
     * Initiates immediate synchronization of local copy of bucket with remote storage in case of both conditions bellow are {@code true}:
     * <ul>
     *     <li>Accumulated amount of locally consumed tokens without external synchronization is greater than or equal to {@code unsynchronizedTokens}</li>
     *     <li>Time passed since last synchronization with external storage is greater than or equal to {@code timeSinceLastSync}</li>
     * </ul>
     *
     * @param unsynchronizedTokens criterion for accumulated amount of unsynchronized tokens
     * @param timeSinceLastSync criterion for time passed since last synchronization
     */
    void syncByCondition(long unsynchronizedTokens, Duration timeSinceLastSync);

}
