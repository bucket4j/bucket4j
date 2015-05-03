/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j;

import com.github.bucket4j.grid.GridBucket;
import com.github.bucket4j.grid.GridBucketState;
import com.github.bucket4j.grid.GridProxy;
import com.github.bucket4j.grid.coherence.CoherenceProxy;
import com.github.bucket4j.grid.hazelcast.HazelcastProxy;
import com.github.bucket4j.grid.ignite.IgniteProxy;
import com.github.bucket4j.local.LockFreeBucket;
import com.hazelcast.core.IMap;
import com.tangosol.net.NamedCache;
import org.apache.ignite.IgniteCache;

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
        return new LockFreeBucket(configuration);
    }

    public Bucket buildHazelcast(IMap<Object, GridBucketState> map, Serializable key) {
        BucketConfiguration configuration = createConfiguration();
        return new GridBucket(configuration, new HazelcastProxy(map, key));
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
