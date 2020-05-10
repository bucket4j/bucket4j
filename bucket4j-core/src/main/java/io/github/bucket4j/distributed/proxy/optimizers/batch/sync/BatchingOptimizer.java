
package io.github.bucket4j.distributed.proxy.optimizers.batch.sync;

import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.RequestOptimizer;
import io.github.bucket4j.distributed.proxy.optimizers.batch.sync.BatchingExecutor;

public class BatchingOptimizer implements RequestOptimizer {

    @Override
    public CommandExecutor optimize(CommandExecutor originalExecutor) {
        return new BatchingExecutor(originalExecutor);
    }

}
