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
import io.github.bucket4j.distributed.AsyncBucketSynchronizationController;
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
import java.util.function.Supplier;

import static io.github.bucket4j.LimitChecker.*;

public class DefaultAsyncBucketProxy implements AsyncBucketProxy, AsyncBucketSynchronizationController {

    private final AsyncCommandExecutor commandExecutor;
    private final Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier;
    private final BucketListener listener;
    private final ImplicitConfigurationReplacement implicitConfigurationReplacement;

    @Override
    public AsyncVerboseBucket asVerbose() {
        return asyncVerboseView;
    }

    @Override
    public AsyncBucketProxy toListenable(BucketListener listener) {
        return new DefaultAsyncBucketProxy(commandExecutor, configurationSupplier, implicitConfigurationReplacement, listener);
    }

    @Override
    public SchedulingBucket asScheduler() {
        return schedulingBucketView;
    }

    public DefaultAsyncBucketProxy(AsyncCommandExecutor commandExecutor, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier, ImplicitConfigurationReplacement implicitConfigurationReplacement, BucketListener listener) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor);
        this.configurationSupplier = configurationSupplier;
        this.implicitConfigurationReplacement = implicitConfigurationReplacement;

        if (listener == null) {
            throw BucketExceptions.nullListener();
        }

        this.listener = listener;
    }

    private final AsyncVerboseBucket asyncVerboseView = new AsyncVerboseBucket() {
        @Override
        public CompletableFuture<VerboseResult<Boolean>> tryConsume(long tokensToConsume) {
            checkTokensToConsume(tokensToConsume);

            VerboseCommand<Boolean> command = TryConsumeCommand.create(tokensToConsume).asVerbose();
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
            VerboseCommand<Long> command = VerboseCommand.from(new ConsumeIgnoringRateLimitsCommand(tokensToConsume));
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

            VerboseCommand<ConsumptionProbe> command = VerboseCommand.from(new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume));
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
            return execute(VerboseCommand.from(new EstimateAbilityToConsumeCommand(numTokens)))
                    .thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossible() {
            VerboseCommand<Long> command = VerboseCommand.from(new ConsumeAsMuchAsPossibleCommand (UNLIMITED_AMOUNT));

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

            VerboseCommand<Long> verboseCommand = VerboseCommand.from(new ConsumeAsMuchAsPossibleCommand(limit));
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
            VerboseCommand<Nothing> verboseCommand = VerboseCommand.from(new AddTokensCommand(tokensToAdd));
            return execute(verboseCommand).thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> forceAddTokens(long tokensToAdd) {
            checkTokensToAdd(tokensToAdd);
            VerboseCommand<Nothing> verboseCommand = VerboseCommand.from(new ForceAddTokensCommand(tokensToAdd));
            return execute(verboseCommand).thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> reset() {
            VerboseCommand<Nothing> verboseCommand = VerboseCommand.from(new ResetCommand());
            return execute(verboseCommand).thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Nothing>> replaceConfiguration(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
            checkConfiguration(newConfiguration);
            checkMigrationMode(tokensInheritanceStrategy);
            VerboseCommand<Nothing> command = VerboseCommand.from(new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy));
            return execute(command).thenApply(RemoteVerboseResult::asLocal);
        }

        @Override
        public CompletableFuture<VerboseResult<Long>> getAvailableTokens() {
            VerboseCommand<Long> command = VerboseCommand.from(new GetAvailableTokensCommand());
            return execute(command).thenApply(RemoteVerboseResult::asLocal);
        }
    };

    private final SchedulingBucket schedulingBucketView = new SchedulingBucket() {
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
        public VerboseSchedulingBucket asVerbose() {
            return verboseSchedulingView;
        }
    };

    private final VerboseSchedulingBucket verboseSchedulingView = new VerboseSchedulingBucket() {
        @Override
        public CompletableFuture<VerboseResult<Boolean>> tryConsume(long tokensToConsume, long maxWaitTimeNanos, ScheduledExecutorService scheduler) {
            checkMaxWaitTime(maxWaitTimeNanos);
            checkTokensToConsume(tokensToConsume);
            checkScheduler(scheduler);
            CompletableFuture<VerboseResult<Boolean>> resultFuture = new CompletableFuture<>();
            ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, maxWaitTimeNanos);
            CompletableFuture<RemoteVerboseResult<Long>> reservationFuture = execute(consumeCommand.asVerbose());
            reservationFuture.whenComplete((RemoteVerboseResult<Long> nanosToSleepVerbose, Throwable exception) -> {
                if (exception != null) {
                    resultFuture.completeExceptionally(exception);
                    return;
                }
                long nanosToSleep = nanosToSleepVerbose.getValue();
                if (nanosToSleep == INFINITY_DURATION) {
                    resultFuture.complete(nanosToSleepVerbose.withValue(false).asLocal());
                    listener.onRejected(tokensToConsume);
                    return;
                }
                if (nanosToSleep == 0L) {
                    resultFuture.complete(nanosToSleepVerbose.withValue(true).asLocal());
                    listener.onConsumed(tokensToConsume);
                    return;
                }
                try {
                    listener.onConsumed(tokensToConsume);
                    listener.onDelayed(nanosToSleep);
                    Runnable delayedCompletion = () -> resultFuture.complete(nanosToSleepVerbose.withValue(true).asLocal());
                    scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
                } catch (Throwable t) {
                    resultFuture.completeExceptionally(t);
                }
            });
            return resultFuture;
        }

        @Override
        public CompletableFuture<VerboseResult<Void>> consume(long tokensToConsume, ScheduledExecutorService scheduler) {
            checkTokensToConsume(tokensToConsume);
            checkScheduler(scheduler);
            CompletableFuture<VerboseResult<Void>> resultFuture = new CompletableFuture<>();
            ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, INFINITY_DURATION);
            CompletableFuture<RemoteVerboseResult<Long>> reservationFuture = execute(consumeCommand.asVerbose());
            reservationFuture.whenComplete((RemoteVerboseResult<Long> nanosToSleepVerbose, Throwable exception) -> {
                if (exception != null) {
                    resultFuture.completeExceptionally(exception);
                    return;
                }
                long nanosToSleep = nanosToSleepVerbose.getValue();
                if (nanosToSleep == INFINITY_DURATION) {
                    resultFuture.completeExceptionally(BucketExceptions.reservationOverflow());
                    return;
                }
                if (nanosToSleep == 0L) {
                    resultFuture.complete(nanosToSleepVerbose.withValue((Void) null).asLocal());
                    listener.onConsumed(tokensToConsume);
                    return;
                }
                try {
                    listener.onConsumed(tokensToConsume);
                    listener.onDelayed(nanosToSleep);
                    Runnable delayedCompletion = () -> resultFuture.complete(nanosToSleepVerbose.withValue((Void) null).asLocal());
                    scheduler.schedule(delayedCompletion, nanosToSleep, TimeUnit.NANOSECONDS);
                } catch (Throwable t) {
                    resultFuture.completeExceptionally(t);
                }
            });
            return resultFuture;
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

        return execute(TryConsumeCommand.create(tokensToConsume)).thenApply(consumed -> {
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
    public AsyncBucketSynchronizationController getSynchronizationController() {
        return this;
    }

    @Override
    public CompletableFuture<Void> syncByCondition(long unsynchronizedTokens, Duration timeSinceLastSync) {
        return execute(new SyncCommand(unsynchronizedTokens, timeSinceLastSync.toNanos())).thenApply(nothing -> null);
    }

    private <T> CompletableFuture<T> execute(RemoteCommand<T> command) {
        RemoteCommand<T> commandToExecute = implicitConfigurationReplacement == null? command :
            new CheckConfigurationVersionAndExecuteCommand<>(command, implicitConfigurationReplacement.getDesiredConfigurationVersion());

        CompletableFuture<CommandResult<T>> futureResult = commandExecutor.executeAsync(commandToExecute);
        return futureResult.thenCompose(cmdResult -> {
            if (!cmdResult.isBucketNotFound() && !cmdResult.isConfigurationNeedToBeReplaced()) {
                T resultDate = cmdResult.getData();
                return CompletableFuture.completedFuture(resultDate);
            }

            // fetch actual configuration
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

            // retry command execution
            return configurationFuture.thenCompose(configuration -> {
                if (configuration == null) {
                    CompletableFuture<T> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(BucketExceptions.nullConfiguration());
                    return failedFuture;
                }
                RemoteCommand<T> initAndExecuteCommand = implicitConfigurationReplacement == null?
                        new CreateInitialStateAndExecuteCommand<>(configuration, command) :
                        new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, command, implicitConfigurationReplacement.getDesiredConfigurationVersion(), implicitConfigurationReplacement.getTokensInheritanceStrategy());

                return commandExecutor.executeAsync(initAndExecuteCommand).thenApply(CommandResult::getData);
            });
        });
    }

}
