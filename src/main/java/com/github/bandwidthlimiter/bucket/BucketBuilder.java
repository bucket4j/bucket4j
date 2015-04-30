package com.github.bandwidthlimiter.bucket;

import com.github.bandwidthlimiter.bucket.grid.GridBucket;
import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridProxy;
import com.github.bandwidthlimiter.bucket.grid.coherence.CoherenceProxy;
import com.github.bandwidthlimiter.bucket.grid.gridgain.GridgainProxy;
import com.github.bandwidthlimiter.bucket.grid.hazelcast.HazelcastProxy;
import com.github.bandwidthlimiter.bucket.grid.ignite.IgniteProxy;
import com.github.bandwidthlimiter.bucket.local.NonSynchronizedBucket;
import com.github.bandwidthlimiter.bucket.local.ThreadSafeBucket;
import com.hazelcast.core.IMap;
import com.tangosol.net.NamedCache;
import org.apache.ignite.IgniteCache;
import org.gridgain.grid.cache.GridCache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class BucketBuilder {

    private TimeMeter timeMeter = TimeMeter.SYSTEM_NANOTIME;
    private List<BandwidthDefinition> bandwidths = new ArrayList<>(1);

    public BucketBuilder(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    public Bucket build() {
        BucketConfiguration configuration = createConfiguration();
        return new ThreadSafeBucket(configuration);
    }

    public Bucket buildLocalNonSynchronized() {
        BucketConfiguration configuration = createConfiguration();
        return new NonSynchronizedBucket(configuration);
    }

    public Bucket buildHazelcast(IMap<Object, GridBucketState> map, Serializable key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new HazelcastProxy(map, key));
    }

    public Bucket buildGridgain(GridCache<Object, GridBucketState> cache, Object key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new GridgainProxy(cache, key));
    }

    public BucketConfiguration createConfiguration() {
        return new BucketConfiguration(this.bandwidths, timeMeter);
    }

    public Bucket buildIgnite(IgniteCache<Object, GridBucketState> cache, Object key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new IgniteProxy(cache, key));
    }

    public Bucket buildCoherence(NamedCache cache, Object key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new CoherenceProxy(cache, key));
    }

    public Bucket buildCustomGrid(GridProxy gridProxy) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, gridProxy);
    }

    public BucketBuilder withGuaranteedBandwidth(long maxCapacity, long period) {
        return withGuaranteedBandwidth(maxCapacity, period, maxCapacity);
    }

    public BucketBuilder withGuaranteedBandwidth(long maxCapacity, long period, long initialCapacity) {
        final BandwidthDefinition bandwidth = new BandwidthDefinition(maxCapacity, initialCapacity, period, true);
        bandwidths.add(bandwidth);
        return this;
    }

    public BucketBuilder withGuaranteedBandwidth(BandwidthAdjuster bandwidthAdjuster, long period, long initialCapacity) {
        final BandwidthDefinition bandwidth = new BandwidthDefinition(bandwidthAdjuster, initialCapacity, period, true);
        bandwidths.add(bandwidth);
        return this;
    }

    public BucketBuilder withLimitedBandwidth(long maxCapacity, long period) {
        return withLimitedBandwidth(maxCapacity, period, maxCapacity);
    }

    public BucketBuilder withLimitedBandwidth(long maxCapacity, long period, long initialCapacity) {
        final BandwidthDefinition bandwidth = new BandwidthDefinition(maxCapacity, initialCapacity, period, false);
        bandwidths.add(bandwidth);
        return this;
    }

    public BucketBuilder withLimitedBandwidth(BandwidthAdjuster bandwidthAdjuster, long period, long initialCapacity) {
        final BandwidthDefinition bandwidth = new BandwidthDefinition(bandwidthAdjuster, initialCapacity, period, false);
        bandwidths.add(bandwidth);
        return this;
    }

    public BandwidthDefinition getBandwidthDefinition(int index) {
        return bandwidths.get(index);
    }

    public TimeMeter getTimeMeter() {
        return timeMeter;
    }

    @Override
    public String toString() {
        return "BucketBuilder{" +
                "timeMeter=" + timeMeter +
                ", bandwidths=" + bandwidths +
                '}';
    }

}
