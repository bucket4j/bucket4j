package io.github.bucket4j.distributed.proxy.synchronization.batch.mock;

import io.github.bucket4j.util.concurrent.batch.AsyncBatchHelper;
import io.github.bucket4j.util.concurrent.batch.BatchHelper;

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

    private BatchHelper<MockCommand, Long, CombinedMockCommand, CombinedResult> syncBatchHelper = BatchHelper.create(
            CombinedMockCommand::new,
            this::executeSync,
            (cmd) -> (Long) this.executeSync(cmd),
            (combinedCommand, combinedResult) -> combinedResult.getResults()
    );

    private AsyncBatchHelper<MockCommand, Long, CombinedMockCommand, CombinedResult> asyncBatchHelper = AsyncBatchHelper.create(
            CombinedMockCommand::new,
            this::executeAsync,
            (cmd) -> (CompletableFuture<Long>) this.executeAsync(cmd),
            (combinedCommand, combinedResult) -> combinedResult.getResults()
    );

    private final MockState state = new MockState();

    public MockState getState() {
        return state;
    }

    public BatchHelper<MockCommand, Long, CombinedMockCommand, CombinedResult> getSyncBatchHelper() {
        return syncBatchHelper;
    }

    public AsyncBatchHelper<MockCommand, Long, CombinedMockCommand, CombinedResult> getAsyncBatchHelper() {
        return asyncBatchHelper;
    }

    private <T> CompletableFuture<T> executeAsync(MockCommand<T> command) {
        return CompletableFuture.supplyAsync(() -> command.apply(state), executor);
    }

    private <T> T executeSync(MockCommand<T> command) {
        return command.apply(state);
    }

}
