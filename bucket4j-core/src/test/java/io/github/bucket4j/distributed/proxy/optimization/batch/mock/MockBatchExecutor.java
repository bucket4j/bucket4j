package io.github.bucket4j.distributed.proxy.optimization.batch.mock;

import io.github.bucket4j.util.concurrent.BatchHelper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


public class MockBatchExecutor {

    static Executor executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    });

    private BatchHelper<MockCommand, Long, CombinedMockCommand, CombinedResult> syncBatchHelper = BatchHelper.sync(
            CombinedMockCommand::new,
            this::executeSync,
            (cmd) -> (Long) this.executeSync(cmd),
            CombinedResult::getResults
    );

    private BatchHelper<MockCommand, Long, CombinedMockCommand, CombinedResult> asyncBatchHelper = BatchHelper.async(
            CombinedMockCommand::new,
            this::executeAsync,
            (cmd) -> (CompletableFuture<Long>) this.executeAsync(cmd),
            CombinedResult::getResults
    );

    private final MockState state = new MockState();

    public MockState getState() {
        return state;
    }

    public BatchHelper<MockCommand, Long, CombinedMockCommand, CombinedResult> getSyncBatchHelper() {
        return syncBatchHelper;
    }

    public BatchHelper<MockCommand, Long, CombinedMockCommand, CombinedResult> getAsyncBatchHelper() {
        return asyncBatchHelper;
    }

    private <T> CompletableFuture<T> executeAsync(MockCommand<T> command) {
        return CompletableFuture.supplyAsync(() -> command.apply(state), executor);
    }

    private <T> T executeSync(MockCommand<T> command) {
        return command.apply(state);
    }

}
