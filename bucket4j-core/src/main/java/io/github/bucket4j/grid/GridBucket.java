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

package io.github.bucket4j.grid;

import io.github.bucket4j.*;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Represents the bucket which state actually stored outside current JVM.
 *
 * @param <K> type of key
 */
public class GridBucket<K extends Serializable> extends AbstractBucket {

    private final K key;
    private final GridProxy<K> gridProxy;
    private final RecoveryStrategy recoveryStrategy;
    private final Supplier<BucketConfiguration> configurationSupplier;

    public static <T extends Serializable> GridBucket<T> createLazyBucket(T key, Supplier<BucketConfiguration> configurationSupplier, GridProxy<T> gridProxy) {
        return new GridBucket<>(BucketListener.NOPE, key, configurationSupplier, gridProxy, RecoveryStrategy.RECONSTRUCT, false);
    }

    public static <T extends Serializable> GridBucket<T> createInitializedBucket(T key, BucketConfiguration configuration, GridProxy<T> gridProxy, RecoveryStrategy recoveryStrategy) {
        return new GridBucket<>(BucketListener.NOPE, key, () -> configuration, gridProxy, recoveryStrategy, true);
    }

    @Override
    public Bucket toListenable(BucketListener listener) {
        return new GridBucket<>(listener, key, configurationSupplier, gridProxy, recoveryStrategy, false);
    }

    private GridBucket(BucketListener listener, K key, Supplier<BucketConfiguration> configurationSupplier, GridProxy<K> gridProxy, RecoveryStrategy recoveryStrategy, boolean initializeBucket) {
        super(listener);
        this.key = key;
        this.gridProxy = gridProxy;
        this.recoveryStrategy = recoveryStrategy;
        this.configurationSupplier = configurationSupplier;
        if (configurationSupplier == null) {
            throw BucketExceptions.nullConfigurationSupplier();
        }
        if (initializeBucket) {
            BucketConfiguration configuration = getConfiguration();
            gridProxy.createInitialState(key, configuration);
        }
    }

    @Override
    public boolean isAsyncModeSupported() {
        return gridProxy.isAsyncModeSupported();
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        return execute(new ConsumeAsMuchAsPossibleCommand(limit));
    }

    @Override
    protected CompletableFuture<Long> tryConsumeAsMuchAsPossibleAsyncImpl(long limit) {
        return executeAsync(new ConsumeAsMuchAsPossibleCommand(limit));
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        return execute(new TryConsumeCommand(tokensToConsume));
    }

    @Override
    protected CompletableFuture<Boolean> tryConsumeAsyncImpl(long tokensToConsume) {
        return executeAsync(new TryConsumeCommand(tokensToConsume));
    }

    @Override
    protected ConsumptionProbe tryConsumeAndReturnRemainingTokensImpl(long tokensToConsume) {
        return execute(new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume));
    }

    @Override
    protected EstimationProbe estimateAbilityToConsumeImpl(long numTokens) {
        return execute(new EstimateAbilityToConsumeCommand(numTokens));
    }

    @Override
    protected CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemainingTokensAsyncImpl(long tokensToConsume) {
        return executeAsync(new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume));
    }

    @Override
    protected CompletableFuture<EstimationProbe> estimateAbilityToConsumeAsyncImpl(long tokensToEstimate) {
        return executeAsync(new EstimateAbilityToConsumeCommand(tokensToEstimate));
    }

    @Override
    protected long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanosLimit) {
        ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, waitIfBusyNanosLimit);
        return execute(consumeCommand);
    }

    @Override
    protected long consumeIgnoringRateLimitsImpl(long tokensToConsume) {
        ConsumeIgnoringRateLimitsCommand command = new ConsumeIgnoringRateLimitsCommand(tokensToConsume);
        return execute(command);
    }

    @Override
    protected VerboseResult<Long> consumeAsMuchAsPossibleVerboseImpl(long limit) {
        ConsumeAsMuchAsPossibleCommand command = new ConsumeAsMuchAsPossibleCommand(limit);
        return execute(command.asVerbose());
    }

    @Override
    protected VerboseResult<Boolean> tryConsumeVerboseImpl(long tokensToConsume) {
        TryConsumeCommand command = new TryConsumeCommand(tokensToConsume);
        return execute(command.asVerbose());
    }

    @Override
    protected VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemainingTokensVerboseImpl(long tokensToConsume) {
        TryConsumeAndReturnRemainingTokensCommand command = new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume);
        return execute(command.asVerbose());
    }

    @Override
    protected VerboseResult<EstimationProbe> estimateAbilityToConsumeVerboseImpl(long numTokens) {
        EstimateAbilityToConsumeCommand command = new EstimateAbilityToConsumeCommand(numTokens);
        return execute(command.asVerbose());
    }

    @Override
    protected VerboseResult<Long> getAvailableTokensVerboseImpl() {
        GetAvailableTokensCommand command = new GetAvailableTokensCommand();
        return execute(command.asVerbose());
    }

    @Override
    protected VerboseResult<Nothing> addTokensVerboseImpl(long tokensToAdd) {
        AddTokensCommand command = new AddTokensCommand(tokensToAdd);
        return execute(command.asVerbose());
    }

    @Override
    protected VerboseResult<Nothing> forceAddTokensVerboseImpl(long tokensToAdd) {
        ForceAddTokensCommand command = new ForceAddTokensCommand(tokensToAdd);
        return execute(command.asVerbose());
    }

    @Override
    protected VerboseResult<Nothing> replaceConfigurationVerboseImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        ReplaceConfigurationCommand replaceConfigCommand = new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy);
        return execute(replaceConfigCommand.asVerbose());
    }

    @Override
    protected VerboseResult<Long> consumeIgnoringRateLimitsVerboseImpl(long tokensToConsume) {
        ConsumeIgnoringRateLimitsCommand command = new ConsumeIgnoringRateLimitsCommand(tokensToConsume);
        return execute(command.asVerbose());
    }

    @Override
    protected CompletableFuture<Long> reserveAndCalculateTimeToSleepAsyncImpl(long tokensToConsume, long maxWaitTimeNanos) {
        ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, maxWaitTimeNanos);
        return executeAsync(consumeCommand);
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        execute(new AddTokensCommand(tokensToAdd));
    }

    @Override
    protected void forceAddTokensImpl(long tokensToAdd) {
        execute(new ForceAddTokensCommand(tokensToAdd));
    }

    @Override
    protected CompletableFuture<Void> addTokensAsyncImpl(long tokensToAdd) {
        CompletableFuture<Nothing> future = executeAsync(new AddTokensCommand(tokensToAdd));
        return future.thenApply(nothing -> null);
    }

    @Override
    protected CompletableFuture<Void> forceAddTokensAsyncImpl(long tokensToAdd) {
        CompletableFuture<Nothing> future = executeAsync(new ForceAddTokensCommand(tokensToAdd));
        return future.thenApply(nothing -> null);
    }

    @Override
    protected void replaceConfigurationImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        ReplaceConfigurationCommand replaceConfigCommand = new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy);
        execute(replaceConfigCommand);
    }

    @Override
    protected CompletableFuture<Nothing> replaceConfigurationAsyncImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        ReplaceConfigurationCommand replaceConfigCommand = new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy);
        return executeAsync(replaceConfigCommand);
    }

    @Override
    protected CompletableFuture<Long> consumeIgnoringRateLimitsAsyncImpl(long tokensToConsume) {
        ConsumeIgnoringRateLimitsCommand command = new ConsumeIgnoringRateLimitsCommand(tokensToConsume);
        return executeAsync(command);
    }

    @Override
    protected CompletableFuture<VerboseResult<Long>> tryConsumeAsMuchAsPossibleVerboseAsyncImpl(long limit) {
        ConsumeAsMuchAsPossibleCommand command = new ConsumeAsMuchAsPossibleCommand(limit);
        return executeAsync(command.asVerbose());
    }

    @Override
    protected CompletableFuture<VerboseResult<Boolean>> tryConsumeVerboseAsyncImpl(long tokensToConsume) {
        TryConsumeCommand command = new TryConsumeCommand(tokensToConsume);
        return executeAsync(command.asVerbose());
    }

    @Override
    protected CompletableFuture<VerboseResult<ConsumptionProbe>> tryConsumeAndReturnRemainingTokensVerboseAsyncImpl(long tokensToConsume) {
        TryConsumeAndReturnRemainingTokensCommand command = new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume);
        return executeAsync(command.asVerbose());
    }

    @Override
    protected CompletableFuture<VerboseResult<EstimationProbe>> estimateAbilityToConsumeVerboseAsyncImpl(long tokensToEstimate) {
        EstimateAbilityToConsumeCommand command = new EstimateAbilityToConsumeCommand(tokensToEstimate);
        return executeAsync(command.asVerbose());
    }

    @Override
    protected CompletableFuture<VerboseResult<Nothing>> addTokensVerboseAsyncImpl(long tokensToAdd) {
        AddTokensCommand addTokensCommand = new AddTokensCommand(tokensToAdd);
        return executeAsync(addTokensCommand.asVerbose());
    }

    @Override
    protected CompletableFuture<VerboseResult<Nothing>> forceAddTokensVerboseAsyncImpl(long tokensToAdd) {
        ForceAddTokensCommand addTokensCommand = new ForceAddTokensCommand(tokensToAdd);
        return executeAsync(addTokensCommand.asVerbose());
    }

    @Override
    protected CompletableFuture<VerboseResult<Nothing>> replaceConfigurationVerboseAsyncImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        ReplaceConfigurationCommand replaceConfigCommand = new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy);
        return executeAsync(replaceConfigCommand.asVerbose());
    }

    @Override
    protected CompletableFuture<VerboseResult<Long>> consumeIgnoringRateLimitsVerboseAsyncImpl(long tokensToConsume) {
        ConsumeIgnoringRateLimitsCommand command = new ConsumeIgnoringRateLimitsCommand(tokensToConsume);
        return executeAsync(command.asVerbose());
    }

    @Override
    public long getAvailableTokens() {
        return execute(new GetAvailableTokensCommand());
    }

    @Override
    public BucketState createSnapshot() {
        return execute(new CreateSnapshotCommand());
    }

    private BucketConfiguration getConfiguration() {
        BucketConfiguration bucketConfiguration = configurationSupplier.get();
        if (bucketConfiguration == null) {
            throw BucketExceptions.nullConfiguration();
        }
        return bucketConfiguration;
    }

    private <T extends Serializable> T execute(GridCommand<T> command) {
        CommandResult<T> result = gridProxy.execute(key, command);
        if (!result.isBucketNotFound()) {
            return result.getData();
        }

        // the bucket was removed or lost, it is need to apply recovery strategy
        if (recoveryStrategy == RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION) {
            throw new BucketNotFoundException(key);
        }

        // retry command execution
        return gridProxy.createInitialStateAndExecute(key, getConfiguration(), command);
    }

    private <T extends Serializable> CompletableFuture<T> executeAsync(GridCommand<T> command) {
        CompletableFuture<CommandResult<T>> futureResult = gridProxy.executeAsync(key, command);
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
            return gridProxy.createInitialStateAndExecuteAsync(key, getConfiguration(), command);
        });
    }

}
