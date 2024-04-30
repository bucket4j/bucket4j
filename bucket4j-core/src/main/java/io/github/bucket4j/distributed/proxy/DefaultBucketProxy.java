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
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.OptimizationController;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.commands.*;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class DefaultBucketProxy extends AbstractBucket implements BucketProxy, OptimizationController {

    private final CommandExecutor commandExecutor;
    private final RecoveryStrategy recoveryStrategy;
    private final Supplier<BucketConfiguration> configurationSupplier;
    private final ImplicitConfigurationReplacement implicitConfigurationReplacement;
    private final AtomicBoolean wasInitialized;

    @Override
    public BucketProxy toListenable(BucketListener listener) {
        return new DefaultBucketProxy(configurationSupplier, commandExecutor, recoveryStrategy, wasInitialized, implicitConfigurationReplacement, listener);
    }

    @Override
    public OptimizationController getOptimizationController() {
        return this;
    }

    @Override
    public void syncByCondition(long unsynchronizedTokens, Duration timeSinceLastSync) {
        execute(new SyncCommand(unsynchronizedTokens, timeSinceLastSync.toNanos()));
    }

    public DefaultBucketProxy(Supplier<BucketConfiguration> configurationSupplier, CommandExecutor commandExecutor, RecoveryStrategy recoveryStrategy,
                              ImplicitConfigurationReplacement implicitConfigurationReplacement, BucketListener listener) {
        this(configurationSupplier, commandExecutor, recoveryStrategy, new AtomicBoolean(false), implicitConfigurationReplacement, listener);
    }

    private DefaultBucketProxy(Supplier<BucketConfiguration> configurationSupplier, CommandExecutor commandExecutor, RecoveryStrategy recoveryStrategy, AtomicBoolean wasInitialized, ImplicitConfigurationReplacement implicitConfigurationReplacement, BucketListener listener) {
        super(listener);

        this.commandExecutor = Objects.requireNonNull(commandExecutor);
        this.recoveryStrategy = Objects.requireNonNull(recoveryStrategy);

        if (configurationSupplier == null) {
            throw BucketExceptions.nullConfigurationSupplier();
        }
        this.configurationSupplier = configurationSupplier;
        this.implicitConfigurationReplacement = implicitConfigurationReplacement;
        this.wasInitialized = wasInitialized;
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        return execute(new ConsumeAsMuchAsPossibleCommand(limit));
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        return execute(TryConsumeCommand.create(tokensToConsume));
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
    protected long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long maxWaitTimeNanos) {
        ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, maxWaitTimeNanos);
        return execute(consumeCommand);
    }

    @Override
    protected VerboseResult<Long> reserveAndCalculateTimeToSleepVerboseImpl(long tokensToConsume, long maxWaitTimeNanos) {
        ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, maxWaitTimeNanos);
        return execute(consumeCommand.asVerbose()).asLocal();
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
    protected void replaceConfigurationImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        execute(new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy));
    }

    @Override
    protected long consumeIgnoringRateLimitsImpl(long tokensToConsume) {
        ConsumeIgnoringRateLimitsCommand command = new ConsumeIgnoringRateLimitsCommand(tokensToConsume);
        return execute(command);
    }

    @Override
    public void reset() {
        ResetCommand command = new ResetCommand();
        execute(command);
    }

    @Override
    public long getAvailableTokens() {
        return execute(new GetAvailableTokensCommand());
    }

    @Override
    protected VerboseResult<Long> consumeAsMuchAsPossibleVerboseImpl(long limit) {
        ConsumeAsMuchAsPossibleCommand command = new ConsumeAsMuchAsPossibleCommand(limit);
        return execute(command.asVerbose()).asLocal();
    }

    @Override
    protected VerboseResult<Boolean> tryConsumeVerboseImpl(long tokensToConsume) {
        TryConsumeCommand command = TryConsumeCommand.create(tokensToConsume);
        return execute(command.asVerbose()).asLocal();
    }

    @Override
    protected VerboseResult<ConsumptionProbe> tryConsumeAndReturnRemainingTokensVerboseImpl(long tokensToConsume) {
        TryConsumeAndReturnRemainingTokensCommand command = new TryConsumeAndReturnRemainingTokensCommand(tokensToConsume);
        return execute(command.asVerbose()).asLocal();
    }

    @Override
    protected VerboseResult<EstimationProbe> estimateAbilityToConsumeVerboseImpl(long numTokens) {
        EstimateAbilityToConsumeCommand command = new EstimateAbilityToConsumeCommand(numTokens);
        return execute(command.asVerbose()).asLocal();
    }

    @Override
    protected VerboseResult<Long> getAvailableTokensVerboseImpl() {
        GetAvailableTokensCommand command = new GetAvailableTokensCommand();
        return execute(command.asVerbose()).asLocal();
    }

    @Override
    protected VerboseResult<Nothing> addTokensVerboseImpl(long tokensToAdd) {
        AddTokensCommand command = new AddTokensCommand(tokensToAdd);
        return execute(command.asVerbose()).asLocal();
    }

    @Override
    protected VerboseResult<Nothing> forceAddTokensVerboseImpl(long tokensToAdd) {
        ForceAddTokensCommand command = new ForceAddTokensCommand(tokensToAdd);
        return execute(command.asVerbose()).asLocal();
    }

    @Override
    protected VerboseResult<Nothing> resetVerboseImpl() {
        ResetCommand command = new ResetCommand();
        return execute(command.asVerbose()).asLocal();
    }

    @Override
    protected VerboseResult<Nothing> replaceConfigurationVerboseImpl(BucketConfiguration newConfiguration, TokensInheritanceStrategy tokensInheritanceStrategy) {
        ReplaceConfigurationCommand replaceConfigCommand = new ReplaceConfigurationCommand(newConfiguration, tokensInheritanceStrategy);
        return execute(replaceConfigCommand.asVerbose()).asLocal();
    }

    @Override
    protected VerboseResult<Long> consumeIgnoringRateLimitsVerboseImpl(long tokensToConsume) {
        ConsumeIgnoringRateLimitsCommand command = new ConsumeIgnoringRateLimitsCommand(tokensToConsume);
        return execute(command.asVerbose()).asLocal();
    }

    private BucketConfiguration getConfiguration() {
        BucketConfiguration bucketConfiguration = configurationSupplier.get();
        if (bucketConfiguration == null) {
            throw BucketExceptions.nullConfiguration();
        }
        return bucketConfiguration;
    }

    private <T> T execute(RemoteCommand<T> command) {
        if (implicitConfigurationReplacement != null) {
            command = new CheckConfigurationVersionAndExecuteCommand<>(command, implicitConfigurationReplacement.getDesiredConfigurationVersion());
        }

        boolean wasInitializedBeforeExecution = wasInitialized.get();
        CommandResult<T> result = commandExecutor.execute(command);
        if (!result.isBucketNotFound() && !result.isConfigurationNeedToBeReplaced()) {
            return result.getData();
        }

        // the bucket was removed or lost, or not initialized yet, or needs to upgrade configuration
        if (result.isBucketNotFound() && recoveryStrategy == RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION && wasInitializedBeforeExecution) {
            throw new BucketNotFoundException();
        }

        // retry command execution
        RemoteCommand<T> initAndExecuteCommand = implicitConfigurationReplacement == null?
                new CreateInitialStateAndExecuteCommand<>(getConfiguration(), command) :
                new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(getConfiguration(), command, implicitConfigurationReplacement.getDesiredConfigurationVersion(), implicitConfigurationReplacement.getTokensInheritanceStrategy());
        CommandResult<T> resultAfterInitialization = commandExecutor.execute(initAndExecuteCommand);
        if (resultAfterInitialization.isBucketNotFound()) {
            throw new IllegalStateException("Bucket is not initialized properly");
        }
        T data = resultAfterInitialization.getData();
        wasInitialized.set(true);
        return data;
    }

}
