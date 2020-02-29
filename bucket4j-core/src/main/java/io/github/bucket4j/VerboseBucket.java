package io.github.bucket4j;

/**
 * TODO write javadocs
 */
public interface VerboseBucket {

    /**
     * TODO write javadocs
     */
    VerboseResult<Boolean> tryConsume(long numTokens);

    /**
     * TODO write javadocs
     */
    VerboseResult<Long> consumeIgnoringRateLimits(long tokens);

    /**
     * TODO write javadocs
     */
    VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemaining(long numTokens);

    /**
     * TODO write javadocs
     */
    VerboseResult<EstimationProbe> estimateAbilityToConsume(long numTokens);

    /**
     * TODO write javadocs
     */
    VerboseResult<Long> tryConsumeAsMuchAsPossible();

    /**
     * TODO write javadocs
     */
    VerboseResult<Long> tryConsumeAsMuchAsPossible(long limit);

    /**
     * TODO write javadocs
     */
    VerboseResult<Void> addTokens(long tokensToAdd);

    /**
     * TODO write javadocs
     */
    VerboseResult<Long> getAvailableTokens();

    /**
     * TODO write javadocs
     */
    VerboseResult<Void> replaceConfiguration(BucketConfiguration newConfiguration);


}
