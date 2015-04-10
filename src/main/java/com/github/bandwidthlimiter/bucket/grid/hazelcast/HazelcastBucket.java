package com.github.bandwidthlimiter.bucket.grid.hazelcast;

import com.github.bandwidthlimiter.bucket.BucketConfiguration;
import com.github.bandwidthlimiter.bucket.BucketState;
import com.github.bandwidthlimiter.bucket.grid.AbstractGridBucket;
import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import com.hazelcast.core.IMap;

import java.io.Serializable;

public class HazelcastBucket extends AbstractGridBucket {

    private final IMap<Object, GridBucketState> map;
    private final Serializable key;

    public HazelcastBucket(BucketConfiguration configuration, IMap<Object, GridBucketState> map, Serializable key) {
        super(configuration);
        this.map = map;
        this.key = key;

        GridBucketState gridState = new GridBucketState(configuration, new BucketState(configuration));
        map.putIfAbsent(key, gridState);
    }

    @Override
    protected <T extends Serializable> T execute(GridCommand<T> command) {
        HazelcastCommand entryProcessor = new HazelcastCommand(command);
        return (T) map.executeOnKey(key, entryProcessor);
    }

}
