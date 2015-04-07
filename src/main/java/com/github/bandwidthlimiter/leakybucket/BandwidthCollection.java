package com.github.bandwidthlimiter.leakybucket;

import com.github.bandwidthlimiter.leakybucket.Bandwidth;

public interface BandwidthCollection {

    Bandwidth[] getBandwidths();

    long getRefillMarker(int bandwidthIndex);

    void setRefillMarker(int bandwidthIndex, long refillMarker);

    long getCurrentSize(int bandwidthIndex);

    void setCurrentSize(int bandwidthIndex, long size);

}