package io.github.bucket4j;

/**
 * Intent of this interface is to provide the verbose version of {@link Bucket} API.
 * All methods obey the same semantic but its results are more verbose.
 */
public interface VerboseBucket {

    /**
     * Does the same that {@link Bucket#tryConsume(long)}
     */
    VerboseResult<Boolean> tryConsume(long numTokens);

    /**
     * Does the same that {@link Bucket#consumeIgnoringRateLimits(long)}
     */
    VerboseResult<Long> consumeIgnoringRateLimits(long tokens);

    /**
     * Does the same that {@link Bucket#tryConsumeAndReturnRemaining(long)}
     */
    VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemaining(long numTokens);

    /**
     * Does the same that {@link Bucket#estimateAbilityToConsume(long)}
     */
    VerboseResult<EstimationProbe> estimateAbilityToConsume(long numTokens);

    /**
     * Does the same that {@link Bucket#tryConsumeAsMuchAsPossible()}
     */
    VerboseResult<Long> tryConsumeAsMuchAsPossible();

    /**
     * Does the same that {@link Bucket#tryConsumeAsMuchAsPossible(long)}
     */
    VerboseResult<Long> tryConsumeAsMuchAsPossible(long limit);

    /**
     * Does the same that {@link Bucket#getAvailableTokens()}
     */
    VerboseResult<Long> getAvailableTokens();

    /**
     * Does the same that {@link Bucket#addTokens(long)}
     */
    VerboseResult<Nothing> addTokens(long tokensToAdd);

    /**
     * Does the same that {@link Bucket#replaceConfiguration(BucketConfiguration)}
     */
    VerboseResult<Nothing> replaceConfiguration(BucketConfiguration newConfiguration);


}
