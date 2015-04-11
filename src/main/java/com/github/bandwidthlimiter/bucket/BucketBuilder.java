package com.github.bandwidthlimiter.bucket;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.gridgain.GridgainBucket;
import com.github.bandwidthlimiter.bucket.grid.hazelcast.HazelcastBucket;
import com.github.bandwidthlimiter.bucket.grid.ignite.IgniteBucket;
import com.github.bandwidthlimiter.bucket.local.ThreadSafeBucket;
import com.github.bandwidthlimiter.bucket.local.UnsafeBucket;
import com.hazelcast.core.IMap;
import org.apache.ignite.IgniteCache;
import org.gridgain.grid.GridException;
import org.gridgain.grid.cache.GridCache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class BucketBuilder {

    private RefillStrategy refillStrategy = GenericCellRateRefillStrategy.INSTANCE;
    private TimeMeter timeMeter = TimeMeter.SYSTEM_NANOTIME;
    private boolean raiseErrorWhenConsumeGreaterThanSmallestBandwidth = false;
    private List<Bandwidth> bandwidths = new ArrayList<>(1);

    public BucketBuilder(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    public Bucket buildLocalThreadSafe() {
        BucketConfiguration configuration = createConfiguration();
        return new ThreadSafeBucket(configuration);
    }

    public Bucket buildLocalUnsafe() {
        BucketConfiguration configuration = createConfiguration();
        return new UnsafeBucket(configuration);
    }

    public Bucket buildHazelcast(IMap<Object, GridBucketState> map, Serializable key) {
        BucketConfiguration configuration = createConfiguration();
        return new HazelcastBucket(configuration, map, key);
    }

    public Bucket buildGridgain(GridCache<Object, GridBucketState> cache, Object key) throws GridException {
        BucketConfiguration configuration = createConfiguration();
        return new GridgainBucket(configuration, cache, key);
    }

    public Bucket buildIgnite(IgniteCache<Object, GridBucketState> cache, Object key) throws GridException {
        BucketConfiguration configuration = createConfiguration();
        return new IgniteBucket(configuration, cache, key);
    }

    public BucketBuilder withGuaranteedBandwidth(long maxCapacity, long period) {
        return withGuaranteedBandwidth(maxCapacity, period, maxCapacity);
    }

    public BucketBuilder withGuaranteedBandwidth(long maxCapacity, long period, long initialCapacity) {
        final Bandwidth bandwidth = new Bandwidth(bandwidths.size(), new ImmutableCapacity(maxCapacity), initialCapacity, period, true);
        bandwidths.add(bandwidth);
        return this;
    }

    public BucketBuilder withGuaranteedDynamicBandwidth(Capacity dynamicMaxCapacity, long period, long initialCapacity) {
        final Bandwidth bandwidth = new Bandwidth(bandwidths.size(), dynamicMaxCapacity, initialCapacity, period, true);
        bandwidths.add(bandwidth);
        return this;
    }

    public BucketBuilder withLimitedBandwidth(long maxCapacity, long period) {
        return withLimitedBandwidth(maxCapacity, period, maxCapacity);
    }

    public BucketBuilder withLimitedBandwidth(long maxCapacity, long period, long initialCapacity) {
        final Bandwidth bandwidth = new Bandwidth(bandwidths.size(), new ImmutableCapacity(maxCapacity), initialCapacity, period, false);
        bandwidths.add(bandwidth);
        return this;
    }

    public BucketBuilder withLimitedDynamicBandwidth(Capacity capacity, long period, long initialCapacity) {
        final Bandwidth bandwidth = new Bandwidth(bandwidths.size(), capacity, initialCapacity, period, false);
        bandwidths.add(bandwidth);
        return this;
    }

    public BucketBuilder raiseErrorWhenConsumeGreaterThanSmallestBandwidth() {
        this.raiseErrorWhenConsumeGreaterThanSmallestBandwidth = true;
        return this;
    }

    public BucketBuilder withCustomRefillStrategy(RefillStrategy refillStrategy) {
        this.refillStrategy = refillStrategy;
        return this;
    }

    public BucketBuilder withGenericCellRateRefillStrategy() {
        this.refillStrategy = GenericCellRateRefillStrategy.INSTANCE;
        return this;
    }

    public BucketConfiguration createConfiguration() {
        Bandwidth[] bandwidths = new Bandwidth[this.bandwidths.size()];
        for (int i = 0; i < bandwidths.length; i++) {
            bandwidths[i] = this.bandwidths.get(i);
        }
        return new BucketConfiguration(bandwidths, raiseErrorWhenConsumeGreaterThanSmallestBandwidth, timeMeter, refillStrategy);
    }

    public Bandwidth getBandwidth(int indexInTheBucket) {
        return bandwidths.get(indexInTheBucket);
    }

}
