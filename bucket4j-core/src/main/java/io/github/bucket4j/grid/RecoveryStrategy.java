
package io.github.bucket4j.grid;

/**
 * Specifies the reaction which should be applied in case of previously saved state of bucket has been lost.
 *
 * The state of bucket can be lost by many reasons, for example:
 * <ul>
 *     <li>Split-brain happen.</li>
 *     <li>The bucket state was stored on single grid node without replication strategy and this node was crashed.</li>
 *     <li>Wrong cache configuration.</li>
 *     <li>Pragmatically errors introduced by vendor.</li>
 *     <li>Human mistake.</li>
 * </ul>
 *
 * Each time when {@link GridBucket} detects that bucket state is missed, it applies this strategy to react.
 */
public enum RecoveryStrategy {

    /**
     * Initialize bucket yet another time.
     * Use this strategy if availability is more preferred than consistency.
     */
    RECONSTRUCT,

    /**
     * Throw {@link BucketNotFoundException}.
     * Use this strategy if consistency is more preferred than availability.
     */
    THROW_BUCKET_NOT_FOUND_EXCEPTION

}
