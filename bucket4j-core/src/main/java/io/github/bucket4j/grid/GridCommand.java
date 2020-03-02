
package io.github.bucket4j.grid;

import java.io.Serializable;

public interface GridCommand<T extends Serializable> extends Serializable {

    T execute(GridBucketState state, long currentTimeNanos);

    boolean isBucketStateModified();

}
