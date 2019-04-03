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

package io.github.bucket4j.remote.commands;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.remote.CommandResult;
import io.github.bucket4j.remote.MutableBucketEntry;
import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;


public class EstimateAbilityToConsumeCommand implements RemoteCommand<EstimationProbe> {

    private static final long serialVersionUID = 1L;

    private long tokensToConsume;

    public EstimateAbilityToConsumeCommand(long tokensToEstimate) {
        this.tokensToConsume = tokensToEstimate;
    }

    @Override
    public CommandResult<EstimationProbe> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);
        long availableToConsume = state.getAvailableTokens();
        if (tokensToConsume <= availableToConsume) {
            return CommandResult.success(EstimationProbe.canBeConsumed(availableToConsume));
        } else {
            long nanosToWaitForRefill = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
            return CommandResult.success(EstimationProbe.canNotBeConsumed(availableToConsume, nanosToWaitForRefill));
        }
    }

}
