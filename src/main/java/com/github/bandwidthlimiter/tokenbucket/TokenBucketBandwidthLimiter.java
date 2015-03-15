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

    private static final int REFILL_DATE_OFFSET = 0;
    private static final int GUARANTEED_OFFSET = 1;
    private static final int FIRST_LIMITED_OFFSET = 2;

    private static final boolean WAIT_IF_BUSY = true;
    private static final boolean NO_WAIT_IF_BUSY = false;
    private static final boolean LIMITED_WAITING = true;
    private static final boolean UNLIMITED_WAITING = false;
    private static final long UNSPECIFIED_WAITING_LIMIT = -1;

    private final NanoTimeWrapper nanoTimeWrapper;
    private final int limitedDimension;
    private final long smallestCapacity;
    private final BandwidthDefinition[] limitedBandwidths;
    private final BandwidthDefinition guaranteedBandwidth;
    private final boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth;

    private final AtomicReference<long[]> stateReference;

    TokenBucketBandwidthLimiter(BandwidthDefinition[] limitedBandwidths, BandwidthDefinition guaranteedBandwidth,
                boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth, NanoTimeWrapper nanoTimeWrapper) {

        checkBandwidths(limitedBandwidths, guaranteedBandwidth);

        this.limitedBandwidths = limitedBandwidths;
        this.guaranteedBandwidth = guaranteedBandwidth;
        this.nanoTimeWrapper = nanoTimeWrapper;
        this.limitedDimension = this.limitedBandwidths.length;
        this.smallestCapacity = getSmallestCapacity(limitedBandwidths);
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = raiseErrorWhenConsumeGreaterThanSmallestBandwidth;
        long[] initialState = buildInitialState(limitedBandwidths, guaranteedBandwidth, nanoTimeWrapper);
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

        final long methodStartNanoTime = isWaitingLimited? nanoTimeWrapper.nanoTime(): 0;
        long currentNanoTime = methodStartNanoTime;
        boolean isFirstCycle = true;

        // Moved out of cycle in order to reduce memory allocation in case of high contention.
        // There are no data-races, due to array is published through happens-before edge on the atomic reference
        final long[] newState = new long[limitedDimension + 2];

        while (true) {
            long[] currentState = stateReference.get();
            long previousRefillNanos = currentState[REFILL_DATE_OFFSET];
            if (isFirstCycle) {
                isFirstCycle = false;
            } else {
                currentNanoTime = nanoTimeWrapper.nanoTime();
            }
            if (!isWaitingLimited && currentNanoTime - methodStartNanoTime >= waitIfBusyNanos) {
                return false;
            }
            newState[REFILL_DATE_OFFSET] = currentNanoTime;

            boolean isPassedByGuarantees = false;
            long waitGuaranteedNanos = Long.MAX_VALUE;
            if (guaranteedBandwidth != null) {
                long guaranteedSize = currentState[GUARANTEED_OFFSET];
                guaranteedSize += guaranteedBandwidth.refill(previousRefillNanos, currentNanoTime);
                guaranteedSize = Math.min(guaranteedSize, guaranteedBandwidth.capacity);
                if (tokensToConsume <= guaranteedSize) {
                    isPassedByGuarantees = true;
                    guaranteedSize -= tokensToConsume;
                    newState[GUARANTEED_OFFSET] = guaranteedSize;
                } else {
                    newState[GUARANTEED_OFFSET] = 0l;
                    if (waitIfBusy && tokensToConsume <= guaranteedBandwidth.capacity) {
                        waitGuaranteedNanos = guaranteedBandwidth.nanosRequiredToRefill(tokensToConsume - guaranteedSize);
                    }
                }
            }

            long methodDuration = currentNanoTime - methodStartNanoTime;
            int countOfSuccessfulyChecked = 0;
            for (int i = 0; i < limitedDimension; i++) {
                long limitedSize = currentState[FIRST_LIMITED_OFFSET + i];
                limitedSize += limitedBandwidths[i].refill(previousRefillNanos, currentNanoTime);
                limitedSize = Math.min(limitedSize, limitedBandwidths[i].capacity);

                if (isPassedByGuarantees) {
                    // validation of limited bandwidth should be skipped, in order to satisfy promises about guaranteed bandwidth
                    limitedSize -= tokensToConsume;
                    limitedSize = Math.max(0, limitedSize);
                    newState[FIRST_LIMITED_OFFSET + i] = limitedSize;
                    countOfSuccessfulyChecked++;
                    continue;
                }

                if (tokensToConsume <= limitedSize) {
                    // Limit of current bandwidth successfully checked
                    limitedSize -= tokensToConsume;
                    newState[FIRST_LIMITED_OFFSET + i] = limitedSize;
                    countOfSuccessfulyChecked++;
                    continue;
                }

                if (!waitIfBusy) {
                    // limit is reached and client does not want to wait
                    return false;
                }

                // calculate time required to refill current bandwidth
                long waitLimitedNanos = limitedBandwidths[i].nanosRequiredToRefill(tokensToConsume - tokensToConsume);

                // Check that waiting for refilling is make sense
                if (isWaitingLimited
                        && methodDuration + waitLimitedNanos >= waitIfBusyNanos
                        && (guaranteedBandwidth == null || methodDuration + waitGuaranteedNanos >= waitIfBusyNanos)) {
                    // there is no sense in waiting, due to waiting limit will be exceeded before required counts of tokens will be added to bucket
                    return false;
                }

                // Choose target for waiting
                if (guaranteedBandwidth != null && waitGuaranteedNanos < waitLimitedNanos) {
                    guaranteedBandwidth.sleep(waitGuaranteedNanos);
                } else {
                    limitedBandwidths[i].sleep(waitLimitedNanos);
                }
                break;
            }

            if (countOfSuccessfulyChecked == limitedDimension && stateReference.compareAndSet(currentState, newState)) {
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

    private static long[] buildInitialState(BandwidthDefinition[] limitedBandwidths, BandwidthDefinition garantedBandwidth, NanoTimeWrapper nanoTimeWrapper) {
        long[] state = new long[limitedBandwidths.length + 2];
        if (garantedBandwidth != null) {
            state[GUARANTEED_OFFSET] = garantedBandwidth.initialCapacity;
        }
        for (int i = 0; i < limitedBandwidths.length; i++) {
            state[FIRST_LIMITED_OFFSET + i] = limitedBandwidths[i].initialCapacity;
        }
        state[REFILL_DATE_OFFSET] = nanoTimeWrapper.nanoTime();
        return state;
    }

    private void checkBandwidths(BandwidthDefinition[] limitedBandwidths, BandwidthDefinition guaranteedBandwidth) {
        if (limitedBandwidths.length == 0) {
            throw TokenBucketExceptions.restrictionsNotSpecified();
        }
        for (int i = 0; i < limitedBandwidths.length - 1; i++) {
            for (int j = 1; j < limitedBandwidths.length; j++) {
                BandwidthDefinition first = limitedBandwidths[i];
                BandwidthDefinition second = limitedBandwidths[i];
                if (first.periodInNanos < second.periodInNanos
                        && first.capacity >= second.capacity) {
                    throw TokenBucketExceptions.hasSmallerPeriodButHigherCapacity(first, second);
                }
            }
        }
        if (guaranteedBandwidth == null) {
            for (BandwidthDefinition limited : limitedBandwidths) {
                if (limited.tokensGeneratedInOneNanosecond <= guaranteedBandwidth.tokensGeneratedInOneNanosecond
                        || limited.nanosecondsToGenerateOneToken > guaranteedBandwidth.nanosecondsToGenerateOneToken) {
                    throw guarantedHasGreaterRateThanLimited(guaranteedBandwidth, limited);
                }
            }
        }
    }

}