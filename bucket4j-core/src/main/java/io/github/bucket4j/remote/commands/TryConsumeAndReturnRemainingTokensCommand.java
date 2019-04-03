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

package io.github.bucket4j.remote.commands;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.MutableBucketEntry;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;


public class TryConsumeAndReturnRemainingTokensCommand implements RemoteCommand<ConsumptionProbe> {

    private static final long serialVersionUID = 42;

    private long tokensToConsume;

    public TryConsumeAndReturnRemainingTokensCommand(long tokensToConsume) {
        this.tokensToConsume = tokensToConsume;
    }

    @Override
    public CommandResult<ConsumptionProbe> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            state.consume(tokensToConsume);
            mutableEntry.set(state);
            return CommandResult.success(ConsumptionProbe.consumed(availableToConsume - tokensToConsume));
        } else {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
            return CommandResult.success(ConsumptionProbe.rejected(availableToConsume, nanosToWaitForRefill));
        }
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

}
