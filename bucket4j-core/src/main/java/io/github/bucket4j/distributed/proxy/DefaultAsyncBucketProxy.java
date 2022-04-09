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

package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.AsyncOptimizationController;
import io.github.bucket4j.distributed.AsyncVerboseBucket;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.RemoteVerboseResult;
import io.github.bucket4j.distributed.remote.commands.*;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static io.github.bucket4j.LimitChecker.*;

public class DefaultAsyncBucketProxy implements AsyncBucketProxy, AsyncOptimizationController, SchedulingBucket {

    private final AsyncCommandExecutor commandExecutor;
    private final RecoveryStrategy recoveryStrategy;
    private final Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier;
    private final BucketListener listener;
    private final AtomicBoolean wasInitialized;

    @Override
    public AsyncVerboseBucket asVerbose() {
        return asyncVerboseView;
    }

    @Override
    public AsyncBucketProxy toListenable(BucketListener listener) {
        return new DefaultAsyncBucketProxy(commandExecutor, recoveryStrategy, configurationSupplier, wasInitialized, listener);
    }

    @Override
    public SchedulingBucket asScheduler() {
        return this;
    }

    public DefaultAsyncBucketProxy(AsyncCommandExecutor commandExecutor, RecoveryStrategy recoveryStrategy, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier) {
        this(commandExecutor, recoveryStrategy, configurationSupplier, new AtomicBoolean(false), BucketListener.NOPE);
    }

    private DefaultAsyncBucketProxy(AsyncCommandExecutor commandExecutor, RecoveryStrategy recoveryStrategy, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier, AtomicBoolean wasInitialized, BucketListener listener) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor);
        this.recoveryStrategy = recoveryStrategy;
        this.configurationSupplier = configurationSupplier;
        this.wasInitialized = wasInitialized;

        if (listener == null) {
            throw BucketExceptions.nullListener();
        }

        this.listener = listener;
    }

    private final AsyncVerboseBucket asyncVerboseView = new AsyncVerboseBucket() {
        @Override
        public CompletableFuture<VerboseResult<Boolean>> tryConsume(long tokensToConsume) {
            checkTokensToConsume(tokensToConsume);

            VerboseCommand<Boolean> command = new VerboseCommand<>(new TryConsumeCommand(tokensToConsume));
            return execute(command).thenApply(consumed -> {
                if (consumed.getValue()) {
                    listener.onConsumed(tokensToConsume);
                } else {
                    listener.onRejected(tokensToConsume);
                }
                return consumed.asLocal();
            });
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> consumeIgnoringRateLimits(long tokensToConsume) {
            checkTokensToConsume(tokensToConsume);
            VerboseCommand<Long> command = new VerboseCommand<>(new ConsumeIgnoringRateLimitsCommand(tokensToConsume));
            return execute(command).thenApply(penaltyNanos -> {
                if (penaltyNanos.getValue() == INFINITY_DURATION) {
                    throw BucketExceptions.reservationOverflow();
                }
                listener.onConsumed(tokensToConsume);
                return penaltyNanos.asLocal();
            });
        }

        @Override
        public CompletableFuture<VerboseResult<ConsumptionProbe>> tryConsumeAndReturnRemaining(long tokensToConsume) {
            checkTokensToConsume(tokensToConsume);

            VerboseCommand<ConsumptionProbe> command = new VerboseCommand<>(new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume));
            return execute(command).thenApply(probe -> {
                if (probe.getValue().isConsumed()) {
                    listener.onConsumed(tokensToConsume);
                } else {
                    listener.onRejected(tokensToConsume);
                }
                return probe.asLocal();
            });
        }

        @Override
        public CompletableFuture<VerboseResult<EstimationProbe>> estimateAbilityToConsume(long numTokens) {
            checkTokensToConsume(numTokens);
            return execute(new VerboseCommand<>(new EstimateAbilityToConsumeCommand(numTokens)))
                    .thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossible() {
            VerboseCommand<Long> command = new VerboseCommand<>(new ConsumeAsMuchAsPossibleCommand (UNLIMITED_AMOUNT));

            return execute(command).thenApply(consumedTokens -> {
                long actuallyConsumedTokens = consumedTokens.getValue();
                if (actuallyConsumedTokens > 0) {
                    listener.onConsumed(actuallyConsumedTokens);
                }
                return consumedTokens.asLocal();
            });
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossible(long limit) {
            checkTokensToConsume(limit);

            VerboseCommand<Long> verboseCommand = new VerboseCommand<>(new ConsumeAsMuchAsPossibleCommand(limit));
            return execute(verboseCommand).thenApply(consumedTokens -> {
                long actuallyConsumedTokens = consumedTokens.getValue();
                if (actuallyConsumedTokens > 0) {
                    listener.onConsumed(actuallyConsumedTokens);
                }
                return consumedTokens.asLocal();
            });
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> addTokens(long tokensToAdd) {
            checkTokensToAdd(tokensToAdd);
            VerboseCommand<Nothing> verboseCommand = new VerboseCommand<>(new AddTokensCommand(tokensToAdd));
            return execute(verboseCommand).thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> forceAddTokens(long tokensToAdd) {
            checkTokensToAdd(tokensToAdd);
            VerboseCommand<Nothing> verboseCommand = new VerboseCommand<>(new ForceAddTokensCommand(tokensToAdd));
            return execute(verboseCommand).thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> reset() {
            VerboseCommand<Nothing> verboseCommand = new VerboseCommand<>(new ResetCommand());
            return execute(verboseCommand).thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
            checkConfiguration(newConfiguration);
            checkMigrationMode(tokensInheritanceStrategy);
            VerboseCommand<Nothing> command = new VerboseCommand<>(new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy));
            return execute(command).thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> getAvailableTokens() {
            VerboseCommand<Long> command = new VerboseCommand<>(new GetAvailableTokensCommand());
            return execute(command).thenApply(RemoteVerboseResult::asLocal);
        }
    };

    @Override
    public CompletableFuture<Long> consumeIgnoringRateLimits(long tokensToConsume) {
        checkTokensToConsume(tokensToConsume);
        return execute(new ConsumeIgnoringRateLimitsCommand(tokensToConsume)).thenApply(penaltyNanos -> {
            if (penaltyNanos == INFINITY_DURATION) {
                throw BucketExceptions.reservationOverflow();
            }
            listener.onConsumed(tokensToConsume);
            return penaltyNanos;
        });
    }

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
                resultFuture.completeExceptionally(BucketExceptions.reservationOverflow());
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
    public CompletableFuture<Void> replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        checkConfiguration(newConfiguration);
        checkMigrationMode(tokensInheritanceStrategy);
        ReplaceConfigurationCommand replaceConfigCommand = new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy);
        CompletableFuture<Nothing> result = execute(replaceConfigCommand);
        return result.thenApply(nothing -> null);
    }

    @Override
    public CompletableFuture<Void> addTokens(long tokensToAdd) {
        checkTokensToAdd(tokensToAdd);
        CompletableFuture<Nothing> future = execute(new AddTokensCommand(tokensToAdd));
        return future.thenApply(nothing -> null);
    }

    @Override
    public CompletableFuture<Void> forceAddTokens(long tokensToAdd) {
        checkTokensToAdd(tokensToAdd);
        CompletableFuture<Nothing> future = execute(new ForceAddTokensCommand(tokensToAdd));
        return future.thenApply(nothing -> null);
    }

    @Override
    public CompletableFuture<Void> reset() {
        CompletableFuture<Nothing> future = execute(new ResetCommand());
        return future.thenApply(nothing -> null);
    }

    @Override
    public CompletableFuture<Long> getAvailableTokens() {
        return execute(new GetAvailableTokensCommand());
    }

    @Override
    public AsyncOptimizationController getOptimizationController() {
        return this;
    }

    @Override
    public CompletableFuture<Void> syncByCondition(long unsynchronizedTokens, Duration timeSinceLastSync) {
        return execute(new SyncCommand(unsynchronizedTokens, timeSinceLastSync.toNanos())).thenApply(nothing -> null);
    }

    private <T> CompletableFuture<T> execute(RemoteCommand<T> command) {
        boolean wasInitializedBeforeExecution = wasInitialized.get();
        CompletableFuture<CommandResult<T>> futureResult = commandExecutor.executeAsync(command);
        return futureResult.thenCompose(cmdResult -> {
            if (!cmdResult.isBucketNotFound()) {
                T resultDate = cmdResult.getData();
                return CompletableFuture.completedFuture(resultDate);
            }
            if (recoveryStrategy == RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION && wasInitializedBeforeExecution) {
                CompletableFuture<T> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new BucketNotFoundException());
                return failedFuture;
            }

            CompletableFuture<BucketConfiguration> configurationFuture;
            try {
                configurationFuture = configurationSupplier.get();
            } catch (Throwable t) {
                CompletableFuture<T> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(t);
                return failedFuture;
            }
            if (configurationFuture == null) {
                CompletableFuture<T> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(BucketExceptions.nullConfigurationFuture());
                return failedFuture;
            }

            return configurationFuture.thenCompose(configuration -> {
                if (configuration == null) {
                    CompletableFuture<T> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(BucketExceptions.nullConfiguration());
                    return failedFuture;
                }
                CreateInitialStateAndExecuteCommand<T> initAndExecute = new CreateInitialStateAndExecuteCommand<>(configuration, command);
                return commandExecutor.executeAsync(initAndExecute).thenApply(initAndExecuteCmdResult -> {
                    wasInitialized.set(true);
                    return initAndExecuteCmdResult.getData();
                });
            });
        });
    }

}
