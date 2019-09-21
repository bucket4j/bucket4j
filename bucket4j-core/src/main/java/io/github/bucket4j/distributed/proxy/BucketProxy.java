/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.distributed.proxy;

import io.github.bucket4j.*;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.commands.*;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Represents the bucket which state actually stored outside current JVM.
 *
 * @param <K> type of key
 */
public class BucketProxy<K extends Serializable> extends AbstractBucket {

    private final K key;
    private final CommandExecutor<K> backend;
    private final RecoveryStrategy recoveryStrategy;
    private final Supplier<BucketConfiguration> configurationSupplier;

    public static <T extends Serializable> BucketProxy<T> createLazyBucket(T key, Supplier<BucketConfiguration> configurationSupplier, CommandExecutor<T> backend) {
        return new BucketProxy<>(BucketListener.NOPE, key, configurationSupplier, backend, RecoveryStrategy.RECONSTRUCT, false);
    }

    public static <T extends Serializable> BucketProxy<T> createInitializedBucket(T key, Supplier<BucketConfiguration> configurationSupplier, CommandExecutor<T> backend, RecoveryStrategy recoveryStrategy) {
        return new BucketProxy<>(BucketListener.NOPE, key, configurationSupplier, backend, recoveryStrategy, true);
    }

    @Override
    public Bucket toListenable(BucketListener listener) {
        return new BucketProxy<>(listener, key, configurationSupplier, backend, recoveryStrategy, false);
    }

    private BucketProxy(BucketListener listener, K key, Supplier<BucketConfiguration> configurationSupplier, CommandExecutor<K> backend, RecoveryStrategy recoveryStrategy, boolean initializeBucket) {
        super(listener);
        this.key = key;
        this.backend = backend;
        this.recoveryStrategy = recoveryStrategy;
        this.configurationSupplier = configurationSupplier;
        if (configurationSupplier == null) {
            throw BucketExceptions.nullConfigurationSupplier();
        }
        if (initializeBucket) {
            BucketConfiguration configuration = getConfiguration();
            backend.execute(key, new CreateInitialStateCommand(configuration));
        }
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        return execute(new ConsumeAsMuchAsPossibleCommand(limit));
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        return execute(new TryConsumeCommand(tokensToConsume));
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
    protected long reserveAndCalculateTimeToSleepImpl(long tokensToConsume, long waitIfBusyNanosLimit) {
        ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, waitIfBusyNanosLimit);
        return execute(consumeCommand);
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        execute(new AddTokensCommand(tokensToAdd));
    }

    @Override
    protected void replaceConfigurationImpl(BucketConfiguration newConfiguration) {
        ReplaceConfigurationOrReturnPreviousCommand replaceConfigCommand = new ReplaceConfigurationOrReturnPreviousCommand(newConfiguration);
        BucketConfiguration previousConfiguration = execute(replaceConfigCommand);
        if (previousConfiguration != null) {
            throw new IncompatibleConfigurationException(previousConfiguration, newConfiguration);
        }
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

    private <T extends Serializable> T execute(RemoteCommand<T> command) {
        CommandResult<T> result = backend.execute(key, command);
        if (!result.isBucketNotFound()) {
            return result.getData();
        }

        // the bucket was removed or lost, it is need to apply recovery strategy
        if (recoveryStrategy == RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION) {
            throw new BucketNotFoundException(key);
        }

        // retry command execution
        CreateInitialStateAndExecuteCommand<T> initAndExecuteCommand = new CreateInitialStateAndExecuteCommand<>(getConfiguration(), command);
        return backend.execute(key, initAndExecuteCommand).getData();
    }

}
