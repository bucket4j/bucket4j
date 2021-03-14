/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
     * Does the same that {@link Bucket#addTokens(long)}
     */
    CompletableFuture<VerboseResult<Nothing>> forceAddTokens(long tokensToAdd);

    /**
     * Does the same that {@link Bucket#replaceConfiguration(BucketConfiguration, TokensInheritanceStrategy)}
     */
    CompletableFuture<VerboseResult<Nothing>> replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy);

}
