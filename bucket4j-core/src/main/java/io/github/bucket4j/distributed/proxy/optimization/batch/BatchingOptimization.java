
package io.github.bucket4j.distributed.proxy.optimization.batch;

import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;
import io.github.bucket4j.distributed.proxy.optimization.OptimizationListener;

import java.util.Objects;

public class BatchingOptimization implements Optimization {

    private final OptimizationListener listener;

    public BatchingOptimization(OptimizationListener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public Optimization withListener(OptimizationListener listener) {
        Objects.requireNonNull(listener);
        return new BatchingOptimization(listener);
    }

    @Override
    public CommandExecutor apply(CommandExecutor originalExecutor) {
        return new BatchingExecutor(originalExecutor, listener);
    }

    @Override
    public AsyncCommandExecutor apply(AsyncCommandExecutor originalExecutor) {
        return new AsyncBatchingExecutor(originalExecutor, listener);
    }

}
