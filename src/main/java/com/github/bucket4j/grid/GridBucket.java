/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j.grid;

import com.github.bucket4j.AbstractBucket;
import com.github.bucket4j.BucketConfiguration;
import com.github.bucket4j.BucketState;

public class GridBucket extends AbstractBucket {

    private final GridProxy gridProxy;

    public GridBucket(BucketConfiguration configuration, GridProxy gridProxy) {
        super(configuration);
        this.gridProxy = gridProxy;
        GridBucketState initialState = new GridBucketState(configuration, BucketState.createInitialState(configuration));
        gridProxy.setInitialState(initialState);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        return gridProxy.execute(new ConsumeAsMuchAsPossibleCommand(limit));
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        return gridProxy.execute(new TryConsumeCommand(tokensToConsume));
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyTimeLimit) throws InterruptedException {
        final boolean isWaitingLimited = waitIfBusyTimeLimit > 0;
        final ConsumeOrCalculateTimeToCloseDeficitCommand consumeCommand = new ConsumeOrCalculateTimeToCloseDeficitCommand(tokensToConsume);
        final long methodStartTime = isWaitingLimited? configuration.getTimeMeter().currentTime(): 0;

        while (true) {
            long timeToCloseDeficit = gridProxy.execute(consumeCommand);
            if (timeToCloseDeficit == 0) {
                return true;
            }
            if (timeToCloseDeficit == Long.MAX_VALUE) {
                return false;
            }

            if (isWaitingLimited) {
                long currentTime = configuration.getTimeMeter().currentTime();
                long methodDuration = currentTime - methodStartTime;
                if (methodDuration >= waitIfBusyTimeLimit) {
                    return false;
                }
                long sleepingTimeLimit = waitIfBusyTimeLimit - methodDuration;
                if (timeToCloseDeficit >= sleepingTimeLimit) {
                    return false;
                }
            }
            configuration.getTimeMeter().sleep(timeToCloseDeficit);
        }
    }

    @Override
    public BucketState createSnapshot() {
        long[] snapshotBytes = gridProxy.execute(new CreateSnapshotCommand());
        return new BucketState(snapshotBytes);
    }

}
