
package io.github.bucket4j.distributed.proxy.optimization.batch;

import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.Optimization;

public class BatchingOptimization implements Optimization {

    @Override
    public CommandExecutor apply(CommandExecutor originalExecutor) {
        return new BatchingExecutor(originalExecutor);
    }

    @Override
    public AsyncCommandExecutor apply(AsyncCommandExecutor originalExecutor) {
        return new AsyncBatchingExecutor(originalExecutor);
    }

}
