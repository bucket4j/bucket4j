
package io.github.bucket4j.distributed.proxy.optimizers.batch.async;

import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.AsyncRequestOptimizer;
import io.github.bucket4j.distributed.proxy.optimizers.batch.async.AsyncBatchingExecutor;

public class AsyncBatchingOptimizer implements AsyncRequestOptimizer {

    @Override
    public AsyncCommandExecutor optimize(AsyncCommandExecutor originalExecutor) {
        return new AsyncBatchingExecutor(originalExecutor);
    }

}
