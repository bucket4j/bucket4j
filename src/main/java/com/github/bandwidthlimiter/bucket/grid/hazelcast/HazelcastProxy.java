package com.github.bandwidthlimiter.bucket.grid.hazelcast;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import com.github.bandwidthlimiter.bucket.grid.GridProxy;
import com.hazelcast.core.IMap;

import java.io.Serializable;

public class HazelcastProxy implements GridProxy {

    private final IMap<Object, GridBucketState> map;
    private final Serializable key;

    public HazelcastProxy(IMap<Object, GridBucketState> map, Serializable key) {
        this.map = map;
        this.key = key;
    }

    @Override
    public <T extends Serializable> T execute(GridCommand<T> command) {
        HazelcastCommand entryProcessor = new HazelcastCommand(command);
        return (T) map.executeOnKey(key, entryProcessor);
    }

    @Override
    public void setInitialState(GridBucketState initialState) {
        map.putIfAbsent(key, initialState);
    }

}
