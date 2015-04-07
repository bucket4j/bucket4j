package com.github.bandwidthlimiter.leakybucket.genericcellrate;

import com.github.bandwidthlimiter.leakybucket.Bandwidth;
import com.github.bandwidthlimiter.leakybucket.BandwidthCollection;
import com.github.bandwidthlimiter.leakybucket.RefillStrategy;

import java.util.concurrent.TimeUnit;

public class GenericCellRateRefillStrategy implements RefillStrategy {

    public static final RefillStrategy INSTANCE = new GenericCellRateRefillStrategy();

    @Override
    public void setupInitialState(BandwidthCollection collection, long currentTime) {
        Bandwidth[] bandwidths = collection.getBandwidths();
        for (int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            collection.setCurrentSize(i, bandwidth.getInitialCapacity());
            collection.setRefillMarker(i, currentTime);
        }
    }

    @Override
    public void refill(BandwidthCollection collection, long currentTime) {
        Bandwidth[] bandwidths = collection.getBandwidths();
        for (int i = 0; i < bandwidths.length; i++) {
            Bandwidth bandwidth = bandwidths[i];
            long previousRefillTime = collection.getRefillMarker(i);
            final long maxCapacity = bandwidth.getMaxCapacity();
            long calculatedRefill = (currentTime - previousRefillTime) * maxCapacity / bandwidth.getPeriodInNanos();
            if (calculatedRefill > 0) {
                long newSize = collection.getCurrentSize(i) + calculatedRefill;
                newSize = Math.min(maxCapacity, newSize);
                collection.setCurrentSize(i, newSize);
                collection.setRefillMarker(i, currentTime);
            }
        }
    }

    @Override
    public long timeRequiredToRefill(BandwidthCollection collection, int bandwidthIndex, long currentTime, long numTokens) {
        Bandwidth bandwidth = collection.getBandwidths()[bandwidthIndex];
        return bandwidth.getPeriodInNanos() * numTokens / bandwidth.getMaxCapacity();
    }
}
