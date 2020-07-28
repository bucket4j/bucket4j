
package io.github.bucket4j.distributed.proxy.optimizers.batch;

import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.RequestOptimizer;
import io.github.bucket4j.distributed.proxy.optimizers.batch.AsyncBatchingExecutor;
import io.github.bucket4j.distributed.proxy.optimizers.batch.BatchingExecutor;

public class BatchingOptimizer implements RequestOptimizer {

    @Override
    public CommandExecutor optimize(CommandExecutor originalExecutor) {
        return new BatchingExecutor(originalExecutor);
    }

    @Override
    public AsyncCommandExecutor optimize(AsyncCommandExecutor originalExecutor) {
        return new AsyncBatchingExecutor(originalExecutor);
    }

}
