package com.github.bandwidthlimiter.bucket.grid.coherence;

import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import com.github.bandwidthlimiter.bucket.grid.GridProxy;
import com.tangosol.net.NamedCache;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.processor.ConditionalPut;

import java.io.Serializable;

public class CoherenceProxy implements GridProxy {

    private final NamedCache cache;
    private final Object key;

    public CoherenceProxy(NamedCache cache, Object key) {
        this.cache = cache;
        this.key = key;
    }

    @Override
    public <T extends Serializable> T execute(GridCommand<T> command) {
        CoherenceCommand<T> entryProcessor = new CoherenceCommand<>(command);
        return (T) cache.invoke(key, entryProcessor);
    }

    @Override
    public void setInitialState(GridBucketState initialState) {
        NotFilter filter = new NotFilter(PresentFilter.INSTANCE);
        cache.invoke(key, new ConditionalPut(filter, initialState));
    }

}
