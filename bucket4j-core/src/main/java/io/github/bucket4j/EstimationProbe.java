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

package io.github.bucket4j;

import io.github.bucket4j.distributed.AsyncBucketProxy;

import java.io.Serializable;

/**
 * Describes the estimation result.
 *
 * @see Bucket#estimateAbilityToConsume(long)
 * @see AsyncBucketProxy#estimateAbilityToConsume(long)
 */
public class EstimationProbe implements Serializable {

    private static final long serialVersionUID = 42L;

    private final boolean canBeConsumed;
    private final long remainingTokens;
    private final long nanosToWaitForRefill;

    public static EstimationProbe canBeConsumed(long remainingTokens) {
        return new EstimationProbe(true, remainingTokens, 0);
    }

    public static EstimationProbe canNotBeConsumed(long remainingTokens, long nanosToWaitForRefill) {
        return new EstimationProbe(false, remainingTokens, nanosToWaitForRefill);
    }

    private EstimationProbe(boolean canBeConsumed, long remainingTokens, long nanosToWaitForRefill) {
        this.canBeConsumed = canBeConsumed;
        this.remainingTokens = Math.max(0L, remainingTokens);
        this.nanosToWaitForRefill = nanosToWaitForRefill;
    }

    /**
     * Flag describes result of consumption operation.
     *
     * @return true if requested tokens can be consumed
     */
    public boolean canBeConsumed() {
        return canBeConsumed;
    }

    /**
     * Return the tokens remaining in the bucket
     *
     * @return the tokens remaining in the bucket
     */
    public long getRemainingTokens() {
        return remainingTokens;
    }

    /**
     * Returns zero if {@link #canBeConsumed()} returns true, else time in nanos which need to wait until requested amount of tokens will be refilled
     *
     * @return Zero if {@link #canBeConsumed()} returns true, else time in nanos which need to wait until requested amount of tokens will be refilled
     */
    public long getNanosToWaitForRefill() {
        return nanosToWaitForRefill;
    }

    @Override
    public String toString() {
        return "ConsumptionResult{" +
                "isConsumed=" + canBeConsumed +
                ", remainingTokens=" + remainingTokens +
                ", nanosToWaitForRefill=" + nanosToWaitForRefill +
                '}';
    }

}
