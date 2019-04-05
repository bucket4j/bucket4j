/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
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

package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;

public class ReserveAndCalculateTimeToSleepCommand implements RemoteCommand<Long> {

    private static final long serialVersionUID = 42;

    private long tokensToConsume;
    private long waitIfBusyNanosLimit;

    public ReserveAndCalculateTimeToSleepCommand(long tokensToConsume, long waitIfBusyNanosLimit) {
        this.tokensToConsume = tokensToConsume;
        this.waitIfBusyNanosLimit = waitIfBusyNanosLimit;
    }

    @Override
    public CommandResult<Long> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);

        long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
        if (nanosToCloseDeficit == Long.MAX_VALUE || nanosToCloseDeficit > waitIfBusyNanosLimit) {
            return CommandResult.MAX_VALUE;
        } else {
            state.consume(tokensToConsume);
            mutableEntry.set(state);
            return CommandResult.success(nanosToCloseDeficit);
        }
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

    public long getWaitIfBusyNanosLimit() {
        return waitIfBusyNanosLimit;
    }
}
