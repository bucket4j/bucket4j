package com.github.bandwidthlimiter.bucket;

import java.util.List;

import static com.github.bandwidthlimiter.bucket.BucketExceptions.*;

public class BandwidthAlgorithms {

    public static void checkCompatibility(List<BandwidthDefinition> bandwidths) {
        int countOfLimitedBandwidth = 0;
        int countOfGuaranteedBandwidth = 0;
        BandwidthDefinition guaranteedBandwidth = null;

        for (BandwidthDefinition bandwidth : bandwidths) {
            if (bandwidth.limited) {
                countOfLimitedBandwidth++;
            } else {
                guaranteedBandwidth = bandwidth;
                countOfGuaranteedBandwidth++;
            }
        }

        if (countOfLimitedBandwidth == 0) {
            throw restrictionsNotSpecified();
        }

        if (countOfGuaranteedBandwidth > 1) {
            throw onlyOneGuarantedBandwidthSupported();
        }

        for (int i = 0; i < bandwidths.size() - 1; i++) {
            BandwidthDefinition first = bandwidths.get(i);
            if (first.guaranteed) {
                continue;
            }
            if (first.hasDynamicCapacity()) {
                continue;
            }
            for (int j = i + 1; j < bandwidths.size(); j++) {
                BandwidthDefinition second = bandwidths.get(j);
                if (second.guaranteed) {
                    continue;
                }
                if (second.hasDynamicCapacity()) {
                    continue;
                }
                if (first.period < second.period && first.capacity >= second.capacity) {
                    throw hasOverlaps(first, second);
                } else if (first.period == second.period) {
                    throw hasOverlaps(first, second);
                } else if (first.period > second.period && first.capacity <= second.capacity) {
                    throw hasOverlaps(first, second);
                }
            }
        }

        if (guaranteedBandwidth == null) {
            return;
        }
        if (guaranteedBandwidth.hasDynamicCapacity()) {
            return;
        }
        for (BandwidthDefinition bandwidth : bandwidths) {
            if (!bandwidth.limited) {
                continue;
            }
            if (bandwidth.hasDynamicCapacity()) {
                continue;
            }
            BandwidthDefinition limited = bandwidth;
            if (limited.getTokensPerTimeUnit() <= guaranteedBandwidth.getTokensPerTimeUnit()
                    || limited.getTimeUnitsPerToken() > guaranteedBandwidth.getTimeUnitsPerToken()) {
                throw guarantedHasGreaterRateThanLimited(guaranteedBandwidth, limited);
            }
        }
    }

    public static long getAvailableTokens(Bandwidth[] bandwidths, BucketState state) {
        long availableByLimitation = Long.MAX_VALUE;
        long availableByGuarantee = 0;
        for (Bandwidth bandwidth : bandwidths) {
            if (bandwidth.isLimited()) {
                availableByLimitation = Math.min(availableByLimitation, bandwidth.getCurrentSize(state));
            } else {
                availableByGuarantee = bandwidth.getCurrentSize(state);
            }
        }
        return Math.max(availableByLimitation, availableByGuarantee);
    }

    public static void consume(Bandwidth[] bandwidths, BucketState state, long toConsume) {
        for (Bandwidth bandwidth: bandwidths) {
            bandwidth.consume(state, toConsume);
        }
    }

    public static long delayAfterWillBePossibleToConsume(Bandwidth[] bandwidths, BucketState state, long currentTime, long tokensToConsume) {
        long delayAfterWillBePossibleToConsumeLimited = 0;
        long delayAfterWillBePossibleToConsumeGuaranteed = Long.MAX_VALUE;
        for (Bandwidth bandwidth: bandwidths) {
            long delay = bandwidth.delayAfterWillBePossibleToConsume(state, currentTime, tokensToConsume);
            if (bandwidth.isGuaranteed()) {
                if (delay == 0) {
                    return 0;
                } else {
                    delayAfterWillBePossibleToConsumeGuaranteed = delay;
                }
                continue;
            }
            if (delay > delayAfterWillBePossibleToConsumeLimited) {
                delayAfterWillBePossibleToConsumeLimited = delay;
            }
        }
        return Math.min(delayAfterWillBePossibleToConsumeLimited, delayAfterWillBePossibleToConsumeGuaranteed);
    }

    public static BucketState createInitialState(BucketConfiguration configuration) {
        BucketState state = new BucketState(configuration.getStateSize());
        long currentTime = configuration.getTimeMeter().currentTime();
        for (Bandwidth bandwidth: configuration.getBandwidths()) {
            bandwidth.setupInitialState(state);
        }
        state.setRefillTime(currentTime);
        return state;
    }

    public static void refill(Bandwidth[] bandwidths, BucketState state, long currentTime) {
        for (Bandwidth bandwidth: bandwidths) {
            bandwidth.refill(state, currentTime);
        }
        state.setRefillTime(currentTime);
    }

}
