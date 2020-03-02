package io.github.bucket4j;

import java.util.concurrent.CompletableFuture;

/**
 * TODO write javadocs
 */
public interface AsyncVerboseBucket {

    /**
     * TODO write javadocs
     */
    CompletableFuture<VerboseResult<Boolean>> tryConsume(long numTokens);

    /**
     * TODO write javadocs
     */
    CompletableFuture<VerboseResult<Long>> consumeIgnoringRateLimits(long tokens);

    /**
     * TODO write javadocs
     */
    CompletableFuture<VerboseResult<ConsumptionProbe>> tryConsumeAndReturnRemaining(long numTokens);

    /**
     * TODO write javadocs
     */
    CompletableFuture<VerboseResult<EstimationProbe>> estimateAbilityToConsume(long numTokens);

    /**
     * TODO write javadocs
     */
    CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossible();

    /**
     * TODO write javadocs
     */
    CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossible(long limit);

    /**
     * TODO write javadocs
     */
    CompletableFuture<VerboseResult<Nothing>> addTokens(long tokensToAdd);

    /**
     * TODO write javadocs
     */
    CompletableFuture<VerboseResult<Nothing>> replaceConfiguration(BucketConfiguration newConfiguration);

}
