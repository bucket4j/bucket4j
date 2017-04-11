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

import java.io.Serializable;

public class GridBucket extends AbstractBucket {

    private final GridProxy gridProxy;
    private final RecoveryStrategy recoveryStrategy;

    public GridBucket(BucketConfiguration configuration, GridProxy gridProxy, RecoveryStrategy recoveryStrategy) {
        super(configuration);
        this.gridProxy = gridProxy;
        this.recoveryStrategy = recoveryStrategy;
        initializeBucket();
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
                getConfiguration().getTimeMeter().parkUninterruptibly(nanosToSleep);
            } else {
                getConfiguration().getTimeMeter().park(nanosToSleep);
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

    private <T extends Serializable> T execute(GridCommand<T> command) {
        CommandResult<T> result = gridProxy.execute(command);
        if (!result.isBucketNotFound()) {
            return result.getData();
        }

        // the bucket was removed or lost, it is need to apply recovery strategy
        if (recoveryStrategy == RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION) {
            throw new BucketNotFoundException(gridProxy.getBucketKey());
        } else {
            initializeBucket();
        }

        // retry command execution
        result = gridProxy.execute(command);
        if (!result.isBucketNotFound()) {
            return result.getData();
        } else {
            // something wrong goes with GRID, reinitialization of bucket has no effect
            throw new BucketNotFoundException(gridProxy.getBucketKey());
        }
    }

    private void initializeBucket() {
        BucketConfiguration configuration = getConfiguration();
        GridBucketState initialState = new GridBucketState(configuration, BucketState.createInitialState(configuration));
        gridProxy.setInitialState(initialState);
    }

}
