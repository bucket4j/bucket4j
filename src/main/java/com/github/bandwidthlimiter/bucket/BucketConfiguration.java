package com.github.bandwidthlimiter.bucket;

import java.io.Serializable;
import java.util.List;

import static com.github.bandwidthlimiter.bucket.BucketExceptions.*;

public final class BucketConfiguration implements Serializable {

    private final int stateSize;
    private final Bandwidth[] bandwidths;
    private final TimeMeter timeMeter;

    public BucketConfiguration(List<BandwidthDefinition> bandwidthDefinitions, TimeMeter timeMeter) {
        if (timeMeter == null) {
            throw nullTimeMetter();
        }
        this.timeMeter = timeMeter;

        checkCompatibility(bandwidthDefinitions);

        int offset = 0;
        this.bandwidths = new Bandwidth[bandwidthDefinitions.size()];
        for (int i = 0; i < bandwidthDefinitions.size() ; i++) {
            BandwidthDefinition definition = bandwidthDefinitions.get(i);
            Bandwidth bandwidth = definition.createBandwidth(offset);
            this.bandwidths[i] = bandwidth;
            offset += bandwidth.sizeOfState();
        }
        this.stateSize = offset;
    }

    public TimeMeter getTimeMeter() {
        return timeMeter;
    }

    public Bandwidth[] getBandwidths() {
        return bandwidths;
    }

    public Bandwidth getBandwidth(int index) {
        return bandwidths[index];
    }

    public int getStateSize() {
        return stateSize;
    }

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

}
