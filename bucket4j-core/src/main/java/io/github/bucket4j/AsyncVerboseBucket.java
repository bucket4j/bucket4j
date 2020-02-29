package io.github.bucket4j;

import java.util.concurrent.CompletableFuture;

/**
 * TODO write javadocs
 */
public interface AsyncVerboseBucket {

    /**
     * TODO write javadocs
     */
    VerboseResult<CompletableFuture<Boolean>> tryConsume(long numTokens);

    /**
     * TODO write javadocs
     */
    VerboseResult<CompletableFuture<Long>> consumeIgnoringRateLimits(long tokens);

    /**
     * TODO write javadocs
     */
    VerboseResult<CompletableFuture<ConsumptionProbe>> tryConsumeAndReturnRemaining(long numTokens);

    /**
     * TODO write javadocs
     */
    VerboseResult<CompletableFuture<EstimationProbe>> estimateAbilityToConsume(long numTokens);

    /**
     * TODO write javadocs
     */
    VerboseResult<CompletableFuture<Long>> tryConsumeAsMuchAsPossible();

    /**
     * TODO write javadocs
     */
    VerboseResult<CompletableFuture<Long>> tryConsumeAsMuchAsPossible(long limit);

    /**
     * TODO write javadocs
     */
    VerboseResult<CompletableFuture<Void>> addTokens(long tokensToAdd);

    /**
     * TODO write javadocs
     */
    VerboseResult<CompletableFuture<Void>> replaceConfiguration(BucketConfiguration newConfiguration);

}
