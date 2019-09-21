/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.AsyncBucket;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.commands.*;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.github.bucket4j.LimitChecker.*;

public abstract class AsyncBucketProxy<K extends Serializable> implements AsyncBucket, AsyncScheduledBucket {

    private final BucketListener listener;

    public AsyncBucketProxy(BucketListener listener, K key, CommandExecutor<K> backend, RecoveryStrategy recoveryStrategy, Supplier<BucketConfiguration> configurationSupplier) {
        this.key = key;
        this.backend = backend;
        this.recoveryStrategy = recoveryStrategy;
        this.configurationSupplier = configurationSupplier;
        if (listener == null) {
            throw BucketExceptions.nullListener();
        }

        this.listener = listener;
    }

    private final K key;
    private final CommandExecutor<K> backend;
    private final RecoveryStrategy recoveryStrategy;
    private final Supplier<BucketConfiguration> configurationSupplier;

    @Override
    public CompletableFuture<Boolean> tryConsume(long tokensToConsume) {
        checkTokensToConsume(tokensToConsume);

        return execute(new TryConsumeCommand(tokensToConsume)).thenApply(consumed -> {
            if (consumed) {
                listener.onConsumed(tokensToConsume);
            } else {
                listener.onRejected(tokensToConsume);
            }
            return consumed;
        });
    }

    @Override
    public CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemaining(long tokensToConsume) {
        checkTokensToConsume(tokensToConsume);

        return execute(new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume)).thenApply(probe -> {
            if (probe.isConsumed()) {
                listener.onConsumed(tokensToConsume);
            } else {
                listener.onRejected(tokensToConsume);
            }
            return probe;
        });
    }

    @Override
    public CompletableFuture<EstimationProbe> estimateAbilityToConsume(long numTokens) {
        checkTokensToConsume(numTokens);
        return execute(new EstimateAbilityToConsumeCommand(numTokens));
    }

    @Override
    public CompletableFuture<Long> tryConsumeAsMuchAsPossible() {
        return execute(new ConsumeAsMuchAsPossibleCommand(UNLIMITED_AMOUNT)).thenApply(consumedTokens -> {
            if (consumedTokens > 0) {
                listener.onConsumed(consumedTokens);
            }
            return consumedTokens;
        });
    }

    @Override
    public CompletableFuture<Long> tryConsumeAsMuchAsPossible(long limit) {
        checkTokensToConsume(limit);

        return execute(new ConsumeAsMuchAsPossibleCommand(limit)).thenApply(consumedTokens -> {
            if (consumedTokens > 0) {
                listener.onConsumed(consumedTokens);
            }
            return consumedTokens;
        });
    }

    @Override
    public CompletableFuture<Boolean> tryConsume(long tokensToConsume, long maxWaitTimeNanos, ScheduledExecutorService scheduler) {
        checkMaxWaitTime(maxWaitTimeNanos);
        checkTokensToConsume(tokensToConsume);
        checkScheduler(scheduler);
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, maxWaitTimeNanos);
        CompletableFuture<Long> reservationFuture = execute(consumeCommand);
        reservationFuture.whenComplete((nanosToSleep, exception) -> {
            if (exception != null) {
                resultFuture.completeExceptionally(exception);
                return;
            }
            if (nanosToSleep == INFINITY_DURATION) {
                resultFuture.complete(false);
                listener.onRejected(tokensToConsume);
                return;
            }
            if (nanosToSleep == 0L) {
                resultFuture.complete(true);
                listener.onConsumed(tokensToConsume);
                return;
            }
            try {
                listener.onConsumed(tokensToConsume);
                listener.onDelayed(nanosToSleep);
                Runnable delayedCompletion = () -> resultFuture.complete(true);
                scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
            } catch (Throwable t) {
                resultFuture.completeExceptionally(t);
            }
        });
        return resultFuture;
    }

    @Override
    public CompletableFuture<Void> consume(long tokensToConsume, ScheduledExecutorService scheduler) {
        checkTokensToConsume(tokensToConsume);
        checkScheduler(scheduler);
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, INFINITY_DURATION);
        CompletableFuture<Long> reservationFuture = execute(consumeCommand);
        reservationFuture.whenComplete((nanosToSleep, exception) -> {
            if (exception != null) {
                resultFuture.completeExceptionally(exception);
                return;
            }
            if (nanosToSleep == INFINITY_DURATION) {
                String msg = "Existed hardware is unable to service the reservation of so many tokens";
                resultFuture.completeExceptionally(new IllegalStateException(msg));
                return;
            }
            if (nanosToSleep == 0L) {
                resultFuture.complete(null);
                listener.onConsumed(tokensToConsume);
                return;
            }
            try {
                listener.onConsumed(tokensToConsume);
                listener.onDelayed(nanosToSleep);
                Runnable delayedCompletion = () -> resultFuture.complete(null);
                scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
            } catch (Throwable t) {
                resultFuture.completeExceptionally(t);
            }
        });
        return resultFuture;
    }

    @Override
    public CompletableFuture<Void> replaceConfiguration(BucketConfiguration newConfiguration) {
        checkConfiguration(newConfiguration);
        ReplaceConfigurationOrReturnPreviousCommand replaceConfigCommand = new ReplaceConfigurationOrReturnPreviousCommand(newConfiguration);
        CompletableFuture<BucketConfiguration> result = execute(replaceConfigCommand);
        return result.thenCompose(previousConfiguration -> {
            if (previousConfiguration == null) {
                return CompletableFuture.completedFuture(null);
            } else {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new IncompatibleConfigurationException(previousConfiguration, newConfiguration));
                return future;
            }
        });
    }

    @Override
    public CompletableFuture<Void> addTokens(long tokensToAdd) {
        checkTokensToAdd(tokensToAdd);
        CompletableFuture<Nothing> future = execute(new AddTokensCommand(tokensToAdd));
        return future.thenApply(nothing -> null);
    }

    private <T extends Serializable> CompletableFuture<T> execute(RemoteCommand<T> command) {
        CompletableFuture<CommandResult<T>> futureResult = backend.executeAsync(key, command);
        return futureResult.thenCompose(cmdResult -> {
            if (!cmdResult.isBucketNotFound()) {
                T resultDate = cmdResult.getData();
                return CompletableFuture.completedFuture(resultDate);
            }
            if (recoveryStrategy == RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION) {
                CompletableFuture<T> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new BucketNotFoundException(key));
                return failedFuture;
            }
            CreateInitialStateAndExecuteCommand<T> initAndExecute = new CreateInitialStateAndExecuteCommand<>(getConfiguration(), command);
            return backend.executeAsync(key, initAndExecute).thenApply(CommandResult::getData);
        });
    }

    private BucketConfiguration getConfiguration() {
        // TODO
        BucketConfiguration bucketConfiguration = configurationSupplier.get();
        if (bucketConfiguration == null) {
            throw BucketExceptions.nullConfiguration();
        }
        return bucketConfiguration;
    }

}
