package com.github.bandwidthlimiter.bucket.mock;


import com.github.bandwidthlimiter.bucket.grid.GridBucketState;
import com.github.bandwidthlimiter.bucket.grid.GridCommand;
import com.github.bandwidthlimiter.bucket.grid.GridProxy;

import java.io.Serializable;

public class GridProxyMock implements GridProxy {

    private GridBucketState state;

    @Override
    public <T extends Serializable> T execute(GridCommand<T> command) {
        return command.execute(state);
    }

    @Override
    public void setInitialState(GridBucketState initialState) {
        this.state = initialState;
    }

}
