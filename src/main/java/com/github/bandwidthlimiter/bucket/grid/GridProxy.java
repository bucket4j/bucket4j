package com.github.bandwidthlimiter.bucket.grid;

import java.io.Serializable;

public interface GridProxy {

    <T extends Serializable> T execute(GridCommand<T> command);

    void setInitialState(GridBucketState initialState);

}
