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
package io.github.bucket4j.distributed;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.Experimental;
import io.github.bucket4j.LimitChecker;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.SchedulingBucket;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.VerboseResult;

import static io.github.bucket4j.AbstractBucket.completedFuture;

@Experimental
public class AsyncBucketProxyAdapter implements AsyncBucketProxy, AsyncBucketSynchronizationController {

    private final Bucket target;

    private final AsyncVerboseBucket verboseView = new AsyncVerboseBucket() {
        @Override
        public CompletableFuture<VerboseResult<Boolean>> tryConsume(long numTokens) {
            return completedFuture(() -> target.asVerbose().tryConsume(numTokens));
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> consumeIgnoringRateLimits(long tokens) {
            return completedFuture(() -> target.asVerbose().consumeIgnoringRateLimits(tokens));
        }

        @Override
        public CompletableFuture<VerboseResult<ConsumptionProbe>> tryConsumeAndReturnRemaining(long numTokens) {
            return completedFuture(() -> target.asVerbose().tryConsumeAndReturnRemaining(numTokens));
        }

        @Override
        public CompletableFuture<VerboseResult<EstimationProbe>> estimateAbilityToConsume(long numTokens) {
            return completedFuture(() -> target.asVerbose().estimateAbilityToConsume(numTokens));
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossible() {
            return completedFuture(() -> target.asVerbose().tryConsumeAsMuchAsPossible());
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossible(long limit) {
            return completedFuture(() -> target.asVerbose().tryConsumeAsMuchAsPossible(limit));
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> addTokens(long tokensToAdd) {
            return completedFuture(() -> target.asVerbose().addTokens(tokensToAdd));
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> forceAddTokens(long tokensToAdd) {
            return completedFuture(() -> target.asVerbose().forceAddTokens(tokensToAdd));
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> reset() {
            return completedFuture(() -> target.asVerbose().reset());
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
            LimitChecker.checkConfiguration(newConfiguration);
            LimitChecker.checkMigrationMode(tokensInheritanceStrategy);
            return completedFuture(() -> target.asVerbose().replaceConfiguration(newConfiguration, tokensInheritanceStrategy));
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> getAvailableTokens() {
            return completedFuture(() -> target.asVerbose().getAvailableTokens());
        }
    };

    /**
     * TODO
     *
     * @param bucket
     * @return
     */
    public static AsyncBucketProxy fromSync(Bucket bucket) {
        return new AsyncBucketProxyAdapter(bucket);
    }

    public AsyncBucketProxyAdapter(Bucket target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public SchedulingBucket asScheduler() {
        return target.asScheduler();
    }

    @Override
    public AsyncVerboseBucket asVerbose() {
        return verboseView;
    }

    @Override
    public CompletableFuture<Boolean> tryConsume(long numTokens) {
        return completedFuture(() -> target.tryConsume(numTokens));
    }

    @Override
    public CompletableFuture<Long> consumeIgnoringRateLimits(long tokens) {
        return completedFuture(() -> target.consumeIgnoringRateLimits(tokens));
    }

    @Override
    public CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemaining(long numTokens) {
        return completedFuture(() -> target.tryConsumeAndReturnRemaining(numTokens));
    }

    @Override
    public CompletableFuture<EstimationProbe> estimateAbilityToConsume(long numTokens) {
        return completedFuture(() -> target.estimateAbilityToConsume(numTokens));
    }

    @Override
    public CompletableFuture<Long> tryConsumeAsMuchAsPossible() {
        return completedFuture(() -> target.tryConsumeAsMuchAsPossible());
    }

    @Override
    public CompletableFuture<Long> tryConsumeAsMuchAsPossible(long limit) {
        return completedFuture(() -> target.tryConsumeAsMuchAsPossible(limit));
    }

    @Override
    public CompletableFuture<Void> addTokens(long tokensToAdd) {
        return completedFuture(() -> {
            target.addTokens(tokensToAdd);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> forceAddTokens(long tokensToAdd) {
        return completedFuture(() -> {
            target.forceAddTokens(tokensToAdd);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> reset() {
        return completedFuture(() -> {
            target.reset();
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        LimitChecker.checkConfiguration(newConfiguration);
        LimitChecker.checkMigrationMode(tokensInheritanceStrategy);
        return completedFuture(() -> {
            target.replaceConfiguration(newConfiguration, tokensInheritanceStrategy);
            return null;
        });
    }

    @Override
    public CompletableFuture<Long> getAvailableTokens() {
        return completedFuture(() -> target.getAvailableTokens());
    }

    @Override
    public AsyncBucketSynchronizationController getSynchronizationController() {
        return this;
    }

    @Override
    public CompletableFuture<Void> syncImmediately() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> syncByCondition(long unsynchronizedTokens, Duration timeSinceLastSync) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public AsyncBucketProxy toListenable(BucketListener listener) {
        return new AsyncBucketProxyAdapter(target.toListenable(listener));
    }

}
