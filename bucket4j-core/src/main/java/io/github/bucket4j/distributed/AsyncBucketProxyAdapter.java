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

import io.github.bucket4j.*;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.AbstractBucket.completedFuture;

public class AsyncBucketProxyAdapter implements AsyncBucketProxy {

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
        public CompletableFuture<VerboseResult<Nothing>> replaceConfiguration(BucketConfiguration newConfiguration) {
            return completedFuture(() -> target.asVerbose().replaceConfiguration(newConfiguration));
        }
    };

    public AsyncBucketProxyAdapter(Bucket target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public ScheduledBucket asScheduler() {
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
    public CompletableFuture<Void> replaceConfiguration(BucketConfiguration newConfiguration) {
        return completedFuture(() -> {
            target.replaceConfiguration(newConfiguration);
            return null;
        });
    }

    @Override
    public CompletableFuture<Long> getAvailableTokens() {
        return completedFuture(() -> target.getAvailableTokens());
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
