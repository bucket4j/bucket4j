package com.github.bandwidthlimiter.bucket;

import java.io.Serializable;
import java.util.List;

import static com.github.bandwidthlimiter.bucket.BucketExceptions.nullTimeMetter;

public final class BucketConfiguration implements Serializable {

    private final int stateSize;
    private final Bandwidth[] bandwidths;
    private final TimeMeter timeMeter;

    public BucketConfiguration(List<BandwidthDefinition> bandwidthDefinitions, TimeMeter timeMeter) {
        if (timeMeter == null) {
            throw nullTimeMetter();
        }
        this.timeMeter = timeMeter;

        BandwidthAlgorithms.checkCompatibility(bandwidthDefinitions);

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

}
