package com.github.bandwidthlimiter.bucket.grid;

import java.io.Serializable;

public interface GridCommand<T extends Serializable> extends Serializable {

    T execute(GridBucketState state);

    boolean isBucketStateModified();

}
