/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.bandwidthlimiter.tokenbucket;

import com.github.bandwidthlimiter.BandwidthLimiter;
import com.github.bandwidthlimiter.ChuckNorris;
import com.github.bandwidthlimiter.NanoTimeWrapper;

import java.util.concurrent.atomic.AtomicReference;

import static com.github.bandwidthlimiter.tokenbucket.TokenBucketExceptions.*;

/**
 * A token bucket implementation that is of a leaky bucket in the sense that it has a finite capacity and any added
 * tokens that would exceed this capacity will "overflow" out of the bucket and are lost forever.
 * <p/>
 * In this implementation the rules for refilling the bucket are encapsulated in a provided {@code RefillStrategy}
 * instance.  Prior to attempting to consumeSingleToken any tokens the refill strategy will be consulted to see how many tokens
 * should be added to the bucket.
 * <p/>
 * In addition in this implementation the method of yielding CPU control is encapsulated in the provided
 * {@code SleepStrategy} instance.  For high performance applications where tokens are being refilled incredibly quickly
 * and an accurate bucket implementation is required, it may be useful to never yield control of the CPU and to instead
 * busy wait.  This strategy allows the caller to make this decision for themselves instead of the library forcing a
 * decision.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Token_bucket">Token Bucket on Wikipedia</a>
 * @see <a href="http://en.wikipedia.org/wiki/Leaky_bucket">Leaky Bucket on Wikipedia</a>
 */
public class TokenBucketBandwidthLimiter implements BandwidthLimiter {

    private static final int PREVIOUS_REFILL_OFFSET = 0;
    private static final int GUARANTEED_SIZE_OFFSET = 1;
    private static final int FIRST_RESTRICTED_SIZE_OFFSET = 2;

    private static final boolean WAIT_IF_BUSY = true;
    private static final boolean NO_WAIT_IF_BUSY = false;
    private static final boolean LIMITED_WAITING = true;
    private static final boolean UNLIMITED_WAITING = false;
    private static final long UNSPECIFIED_WAITING_LIMIT = -1;

    private final NanoTimeWrapper nanoTimeWrapper;
    private final int restrictedDimension;
    private final long smallestCapacity;
    private final BandwidthDefinition[] restrictedBandwidths;
    private final BandwidthDefinition guaranteedBandwidth;
    private final boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth;

    private final AtomicReference<long[]> stateReference;

    TokenBucketBandwidthLimiter(BandwidthDefinition[] restrictedBandwidths, BandwidthDefinition guaranteedBandwidth,
                boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth, NanoTimeWrapper nanoTimeWrapper) {

        checkBandwidths(restrictedBandwidths, guaranteedBandwidth);

        this.restrictedBandwidths = restrictedBandwidths;
        this.guaranteedBandwidth = guaranteedBandwidth;
        this.nanoTimeWrapper = nanoTimeWrapper;
        this.restrictedDimension = this.restrictedBandwidths.length;
        this.smallestCapacity = getSmallestCapacity(restrictedBandwidths);
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
        long[] initialState = buildInitialState(restrictedBandwidths, guaranteedBandwidth, nanoTimeWrapper);
        this.stateReference = new AtomicReference(initialState);
    }

    @Override
    public boolean tryConsumeSingleToken() {
        return tryConsume(1);
    }

    @Override
    public boolean tryConsume(long numTokens) {
        try {
            return consumeOrAwait(numTokens, NO_WAIT_IF_BUSY, UNLIMITED_WAITING, UNSPECIFIED_WAITING_LIMIT);
        } catch (InterruptedException e) {
            // It should never happen due to waitIfBusy = false
            return ChuckNorris.roundKickExceptionAndGiveMeWhatIWant(e);
        }
    }

    @Override
    public void consumeSingleToken() throws InterruptedException {
        consume(1);
    }

    @Override
    public void consume(long numTokens) throws InterruptedException {
        consumeOrAwait(numTokens, WAIT_IF_BUSY, UNLIMITED_WAITING, UNSPECIFIED_WAITING_LIMIT);
    }

    @Override
    public boolean tryConsumeSingleToken(long maxWaitNanos) throws InterruptedException {
        return tryConsume(1, maxWaitNanos);
    }

    @Override
    public boolean tryConsume(long numTokens, long maxWaitNanos) throws InterruptedException {
        return consumeOrAwait(numTokens, WAIT_IF_BUSY, LIMITED_WAITING, maxWaitNanos);
    }

    private boolean consumeOrAwait(long tokensToConsume, boolean waitIfBusy, boolean isWaitingLimited, long waitIfBusyNanos) throws InterruptedException {
        validateArguments(tokensToConsume, waitIfBusy, isWaitingLimited, waitIfBusyNanos);

        if (tokensToConsume > smallestCapacity) {
            // if this behavior is deprecated then exception already thrown by #validateArguments
            return false;
        }

        final boolean noWaitIfBusy = !waitIfBusy;
        final boolean isWaitingUnlimited = !isWaitingLimited;

        final long methodStartNanoTime = isWaitingLimited? nanoTimeWrapper.nanoTime(): 0;
        long currentNanoTime = methodStartNanoTime;
        boolean isFirstCycle = true;

        // Moved out of cycle in order to reduce memory allocation in case of high contention.
        // There are no data-races, due to array is published through happens-before edge on the atomic reference
        final long[] newState = new long[restrictedDimension + 2];

        while (true) {
            long[] currentState = stateReference.get();
            long previousRefillNanos = getPreviousRefillNanos(currentState);
            if (isFirstCycle) {
                isFirstCycle = false;
            } else {
                currentNanoTime = nanoTimeWrapper.nanoTime();
            }
            if (isWaitingUnlimited && currentNanoTime - methodStartNanoTime >= waitIfBusyNanos) {
                return false;
            }
            setPreviousRefillNanos(newState, currentNanoTime);

            boolean isPassedByGuarantees = false;
            long waitGuaranteedNanos = Long.MAX_VALUE;
            if (guaranteedBandwidth != null) {
                long guaranteedSize = getGuaranteedSize(currentState);
                guaranteedSize += guaranteedBandwidth.refill(previousRefillNanos, currentNanoTime);
                guaranteedSize = Math.min(guaranteedSize, guaranteedBandwidth.capacity);
                if (tokensToConsume <= guaranteedSize) {
                    isPassedByGuarantees = true;
                    guaranteedSize -= tokensToConsume;
                    setGuaranteedSize(newState, guaranteedSize);
                } else {
                    setGuaranteedSize(newState, 0);
                    if (waitIfBusy && tokensToConsume <= guaranteedBandwidth.capacity) {
                        waitGuaranteedNanos = guaranteedBandwidth.nanosRequiredToRefill(tokensToConsume - guaranteedSize);
                    }
                }
            }

            long methodDuration = currentNanoTime - methodStartNanoTime;
            int countOfSuccessfulyChecked = 0;
            for (int i = 0; i < restrictedDimension; i++) {
                long restrictedSize = getRestrictedSize(i, currentState);
                restrictedSize += restrictedBandwidths[i].refill(previousRefillNanos, currentNanoTime);
                restrictedSize = Math.min(restrictedSize, restrictedBandwidths[i].capacity);

                if (isPassedByGuarantees) {
                    // validation of restricted bandwidth should be skipped, in order to satisfy promises about guaranteed bandwidth
                    restrictedSize -= tokensToConsume;
                    restrictedSize = Math.max(0, restrictedSize);
                    setRestrictedSize(i, newState, restrictedSize);
                    countOfSuccessfulyChecked++;
                    continue;
                }

                if (tokensToConsume <= restrictedSize) {
                    // Limit of current bandwidth successfully checked
                    restrictedSize -= tokensToConsume;
                    setRestrictedSize(i, newState, restrictedSize);
                    countOfSuccessfulyChecked++;
                    continue;
                }

                if (noWaitIfBusy) {
                    // limit is reached and client does not want to wait
                    return false;
                }

                // calculate time required to refill current bandwidth
                long waitRestrictedNanos = restrictedBandwidths[i].nanosRequiredToRefill(tokensToConsume - tokensToConsume);

                // Check that waiting for refilling is make sense
                if (isWaitingLimited
                        && methodDuration + waitRestrictedNanos >= waitIfBusyNanos
                        && (guaranteedBandwidth == null || methodDuration + waitGuaranteedNanos >= waitIfBusyNanos)) {
                    // there is no sense in waiting, due to waiting limit will be exceeded before required counts of tokens will be added to bucket
                    return false;
                }

                // Choose target for waiting
                if (guaranteedBandwidth != null && waitGuaranteedNanos < waitRestrictedNanos) {
                    guaranteedBandwidth.sleep(waitGuaranteedNanos);
                } else {
                    restrictedBandwidths[i].sleep(waitRestrictedNanos);
                }
                break;
            }

            if (countOfSuccessfulyChecked == restrictedDimension && stateReference.compareAndSet(currentState, newState)) {
                return true;
            }
        }
    }

    private void validateArguments(long tokensToConsume, boolean waitIfBusy, boolean isWaitingLimited, long waitIfBusyNanos) {
        if (tokensToConsume <= 0) {
            throw nonPositiveTokensToConsume(tokensToConsume);
        }
        if (isWaitingLimited && waitIfBusyNanos <= 0) {
            throw nonPositiveWaitingNanos(waitIfBusyNanos);
        }
        if (tokensToConsume > smallestCapacity) {
            if (waitIfBusy) {
                // there is no sense in waiting, due to limits will be never satisfied
                throw tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(tokensToConsume, smallestCapacity);
            }
            if (raiseErrorWhenConsumeGreaterThanSmallestBandwidth) {
                // illegal api usage detected
                throw tokensToConsumeGreaterThanCapacityOfSmallestBandwidth(tokensToConsume, smallestCapacity);
            }
        }
    }

    private static long getSmallestCapacity(BandwidthDefinition[] definitions) {
        long minCapacity = Long.MAX_VALUE;
        for (int i = 0; i < definitions.length; i++) {
            if (definitions[i].capacity < minCapacity) {
                minCapacity = definitions[i].capacity;
            }
        }
        return minCapacity;
    }

    private static long[] buildInitialState(BandwidthDefinition[] restrictedBandwidths, BandwidthDefinition garantedBandwidth, NanoTimeWrapper nanoTimeWrapper) {
        long[] state = new long[restrictedBandwidths.length + 2];
        if (garantedBandwidth != null) {
            setGuaranteedSize(state, garantedBandwidth.initialCapacity);
        }
        for (int i = 0; i < restrictedBandwidths.length; i++) {
            setRestrictedSize(i, state, restrictedBandwidths[i].initialCapacity);
        }
        setPreviousRefillNanos(state, nanoTimeWrapper.nanoTime());
        return state;
    }

    private void checkBandwidths(BandwidthDefinition[] restricteds, BandwidthDefinition guaranteed) {
        if (restricteds.length == 0) {
            throw TokenBucketExceptions.restrictionsNotSpecified();
        }
        if (guaranteed == null) {
            return;
        }
        for (BandwidthDefinition restricted : restricteds) {
            if (restricted.tokensPerNanosecond <= guaranteed.tokensPerNanosecond
                    || restricted.nanosecondsPerToken > guaranteed.nanosecondsPerToken) {
                throw TokenBucketExceptions.guarantedHasGreaterRateThanRestricted(guaranteed, restricted);
            }
        }
    }

    private static void setPreviousRefillNanos(long[] state, long value) {
        state[PREVIOUS_REFILL_OFFSET] = value;
    }

    private static long getPreviousRefillNanos(long[] state) {
        return state[PREVIOUS_REFILL_OFFSET];
    }

    private static void setGuaranteedSize(long[] state, long value) {
        state[GUARANTEED_SIZE_OFFSET] = value;
    }

    private static long getGuaranteedSize(long[] state) {
        return state[GUARANTEED_SIZE_OFFSET];
    }

    private static void setRestrictedSize(int restrictedIdx, long[] state, long value) {
        state[FIRST_RESTRICTED_SIZE_OFFSET + restrictedIdx] = value;
    }

    private static long getRestrictedSize(int restrictedIdx,long[] state) {
        return state[FIRST_RESTRICTED_SIZE_OFFSET + restrictedIdx];
    }

}