package io.github.bucket4j;

import java.util.concurrent.CompletableFuture;

/**
 * Intent of this interface is to provide the verbose version of {@link AsyncBucket} API.
 * All methods obey the same semantic but its results are more verbose.
 */
public interface AsyncVerboseBucket {

    /**
     * Does the same that {@link Bucket#tryConsume(long)}
     */
    CompletableFuture<VerboseResult<Boolean>> tryConsume(long numTokens);

    /**
     * Does the same that {@link Bucket#consumeIgnoringRateLimits(long)}
     */
    CompletableFuture<VerboseResult<Long>> consumeIgnoringRateLimits(long tokens);

    /**
     * Does the same that {@link Bucket#tryConsumeAndReturnRemaining(long)}
     */
    CompletableFuture<VerboseResult<ConsumptionProbe>> tryConsumeAndReturnRemaining(long numTokens);

    /**
     * Does the same that {@link Bucket#estimateAbilityToConsume(long)}
     */
    CompletableFuture<VerboseResult<EstimationProbe>> estimateAbilityToConsume(long numTokens);

    /**
     * Does the same that {@link Bucket#tryConsumeAsMuchAsPossible()}
     */
    CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossible();

    /**
     * Does the same that {@link Bucket#tryConsumeAsMuchAsPossible(long)}
     */
    CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossible(long limit);

    /**
     * Does the same that {@link Bucket#addTokens(long)}
     */
    CompletableFuture<VerboseResult<Nothing>> addTokens(long tokensToAdd);

    /**
     * Does the same that {@link Bucket#replaceConfiguration(BucketConfiguration)}
     */
    CompletableFuture<VerboseResult<Nothing>> replaceConfiguration(BucketConfiguration newConfiguration);

}
