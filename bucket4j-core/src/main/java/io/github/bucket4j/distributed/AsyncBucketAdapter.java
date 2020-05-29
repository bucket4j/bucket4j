package io.github.bucket4j.distributed;

import io.github.bucket4j.*;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static io.github.bucket4j.AbstractBucket.completedFuture;

public class AsyncBucketAdapter implements AsyncBucket {

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

    public AsyncBucketAdapter(Bucket target) {
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
    public AsyncBucket toListenable(BucketListener listener) {
        return new AsyncBucketAdapter(target.toListenable(listener));
    }

}
