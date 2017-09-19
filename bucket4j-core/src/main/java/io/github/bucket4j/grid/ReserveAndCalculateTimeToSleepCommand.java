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

public class ReserveAndCalculateTimeToSleepCommand implements GridCommand<Long> {

    private static final long serialVersionUID = 1L;

    private long tokensToConsume;
    private long waitIfBusyNanosLimit;
    private boolean bucketStateModified;

    public ReserveAndCalculateTimeToSleepCommand(long tokensToConsume, long waitIfBusyNanosLimit) {
        this.tokensToConsume = tokensToConsume;
        this.waitIfBusyNanosLimit = waitIfBusyNanosLimit;
    }

    @Override
    public Long execute(GridBucketState state, long currentTimeNanos) {
        state.refillAllBandwidth(currentTimeNanos);

        long nanosToCloseDeficit = state.delayNanosAfterWillBePossibleToConsume(tokensToConsume);
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

}
