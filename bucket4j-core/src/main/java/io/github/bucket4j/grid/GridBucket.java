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

package io.github.bucket4j.grid;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.AbstractBucket;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.TimeMeter;

import java.io.Serializable;
import java.util.function.Supplier;

public class GridBucket<K extends Serializable> extends AbstractBucket {

    private final K key;
    private final GridProxy<K> gridProxy;
    private final RecoveryStrategy recoveryStrategy;
    private final Supplier<BucketConfiguration> configurationSupplier;

    public static <T extends Serializable> GridBucket<T> createLazyBucket(T key, Supplier<BucketConfiguration> configurationSupplier, GridProxy<T> gridProxy) {
        return new GridBucket<>(key, configurationSupplier, gridProxy, RecoveryStrategy.RECONSTRUCT, false);
    }

    public static <T extends Serializable> GridBucket<T> createInitializedBucket(T key, BucketConfiguration configuration, GridProxy<T> gridProxy, RecoveryStrategy recoveryStrategy) {
        return new GridBucket<>(key, () -> configuration, gridProxy, recoveryStrategy, true);
    }

    private GridBucket(K key, Supplier<BucketConfiguration> configurationSupplier, GridProxy<K> gridProxy, RecoveryStrategy recoveryStrategy, boolean initializeBucket) {
        this.key = key;
        this.gridProxy = gridProxy;
        this.recoveryStrategy = recoveryStrategy;
        this.configurationSupplier = configurationSupplier;
        if (initializeBucket) {
            initializeBucket();
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
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyNanosLimit, boolean uninterruptibly) throws InterruptedException {
        final ReserveAndCalculateTimeToSleepCommand consumeCommand = new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, waitIfBusyNanosLimit);
        long nanosToSleep = execute(consumeCommand);
        if (nanosToSleep == Long.MAX_VALUE) {
            return false;
        }
        if (nanosToSleep > 0) {
            if (uninterruptibly) {
                TimeMeter.SYSTEM_MILLISECONDS.parkUninterruptibly(nanosToSleep);
            } else {
                TimeMeter.SYSTEM_MILLISECONDS.park(nanosToSleep);
            }
        }
        return true;
    }

    @Override
    protected void addTokensImpl(long tokensToAdd) {
        execute(new AddTokensCommand(tokensToAdd));
    }

    @Override
    public BucketState createSnapshot() {
        return execute(new CreateSnapshotCommand());
    }

    @Override
    public BucketConfiguration getConfiguration() {
        return configurationSupplier.get();
    }

    private <T extends Serializable> T execute(GridCommand<T> command) {
        CommandResult<T> result = gridProxy.execute(key, command);
        if (!result.isBucketNotFound()) {
            return result.getData();
        }

        // the bucket was removed or lost, it is need to apply recovery strategy
        if (recoveryStrategy == RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION) {
            throw new BucketNotFoundException(key);
        } else {
            initializeBucket();
        }

        // retry command execution
        result = gridProxy.execute(key, command);
        if (!result.isBucketNotFound()) {
            return result.getData();
        } else {
            // something wrong goes with GRID, reinitialization of bucket has no effect
            throw new BucketNotFoundException(key);
        }
    }

    private void initializeBucket() {
        BucketConfiguration configuration = getConfiguration();
        GridBucketState initialState = new GridBucketState(configuration, BucketState.createInitialState(configuration));
        gridProxy.setInitialState(key, initialState);
    }

}
