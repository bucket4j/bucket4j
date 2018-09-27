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

import io.github.bucket4j.remote.RemoteBucketState;
import io.github.bucket4j.remote.RemoteCommand;

public class ReserveAndCalculateTimeToSleepCommand implements RemoteCommand<Long> {

    private static final long serialVersionUID = 42;

    private long tokensToConsume;
    private long waitIfBusyNanosLimit;
    private boolean bucketStateModified;
    private Long clientTimeNanos;

    public ReserveAndCalculateTimeToSleepCommand(long tokensToConsume, long waitIfBusyNanosLimit, Long clientTimeNanos) {
        this.tokensToConsume = tokensToConsume;
        this.waitIfBusyNanosLimit = waitIfBusyNanosLimit;
        this.clientTimeNanos = clientTimeNanos;
    }

    @Override
    public Long execute(RemoteBucketState state) {
        long currentTimeNanos = currentTimeNanos();
        state.refillAllBandwidth(currentTimeNanos);

        long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
        if (nanosToCloseDeficit == Long.MAX_VALUE || nanosToCloseDeficit > waitIfBusyNanosLimit) {
            return Long.MAX_VALUE;
        } else {
            state.consume(tokensToConsume);
            bucketStateModified = true;
            return nanosToCloseDeficit;
        }
    }

    @Override
    public boolean isBucketStateModified() {
        return bucketStateModified;
    }

    @Override
    public Long getClientTimeNanos() {
        return clientTimeNanos;
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

    public long getWaitIfBusyNanosLimit() {
        return waitIfBusyNanosLimit;
    }
}
