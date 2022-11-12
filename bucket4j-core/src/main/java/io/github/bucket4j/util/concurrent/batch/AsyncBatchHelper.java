package io.github.bucket4j.util.concurrent.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Helper class for batching
 *
 * @param <T> Task type
 * @param <R> Task result type
 * @param <CT> Combined task type
 * @param <CR> Combined task result
 */
public class AsyncBatchHelper<T, R, CT, CR> {

    private static final WaitingTask<?, ?> QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS = new WaitingTask<>(null);
    private static final WaitingTask<?, ?> QUEUE_EMPTY = new WaitingTask<>(null);

    private final Function<List<T>, CT> taskCombiner;
    private final Function<CT, CompletableFuture<CR>> asyncCombinedTaskExecutor;
    private final Function<T, CompletableFuture<R>> asyncTaskExecutor;
    private final Function<CR, List<R>> combinedResultSplitter;

    private final AtomicReference<WaitingTask> headReference = new AtomicReference<>(QUEUE_EMPTY);

    public static <T, R, CT, CR> AsyncBatchHelper<T, R, CT, CR> create(
            Function<List<T>, CT> taskCombiner,
            Function<CT, CompletableFuture<CR>> asyncCombinedTaskExecutor,
            Function<T, CompletableFuture<R>> asyncTaskExecutor,
            Function<CR, List<R>> combinedResultSplitter) {
        return new AsyncBatchHelper<>(taskCombiner, asyncCombinedTaskExecutor, asyncTaskExecutor, combinedResultSplitter);
    }

    public static <T, R, CT, CR> AsyncBatchHelper<T, R, CT, CR> create(
            Function<List<T>, CT> taskCombiner,
            Function<CT, CompletableFuture<CR>> asyncCombinedTaskExecutor,
            Function<CR, List<R>> combinedResultSplitter
    ) {
        Function<T, CompletableFuture<R>> asyncTaskExecutor = new Function<T, CompletableFuture<R>>() {
            @Override
            public CompletableFuture<R> apply(T task) {
                CT combinedTask = taskCombiner.apply(Collections.singletonList(task));
                CompletableFuture<CR> resultFuture = asyncCombinedTaskExecutor.apply(combinedTask);
                return resultFuture.thenApply((CR combinedResult) -> {
                    List<R> results = combinedResultSplitter.apply(combinedResult);
                    return results.get(0);
                });
            }
        };
        return new AsyncBatchHelper<T, R, CT, CR>(taskCombiner, asyncCombinedTaskExecutor, asyncTaskExecutor, combinedResultSplitter);
    }

    private AsyncBatchHelper(Function<List<T>, CT> taskCombiner,
                        Function<CT, CompletableFuture<CR>> asyncCombinedTaskExecutor,
                        Function<T, CompletableFuture<R>> asyncTaskExecutor,
                        Function<CR, List<R>> combinedResultSplitter) {
        this.taskCombiner = requireNonNull(taskCombiner);
        this.asyncCombinedTaskExecutor = requireNonNull(asyncCombinedTaskExecutor);
        this.asyncTaskExecutor = requireNonNull(asyncTaskExecutor);
        this.combinedResultSplitter = requireNonNull(combinedResultSplitter);
    }

    public CompletableFuture<R> executeAsync(T task) {
        WaitingTask<T, R> waitingTask = lockExclusivelyOrEnqueue(task);

        if (waitingTask != null) {
            // there is another request is in progress, our request will be scheduled later
            return waitingTask.future;
        }

        try {
            return asyncTaskExecutor.apply(task)
                    .whenComplete((result, error) -> scheduleNextBatchAsync());
        } catch (Throwable error) {
            CompletableFuture<R> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(error);
            return failedFuture;
        }
    }

    private void scheduleNextBatchAsync() {
        List<WaitingTask<T, R>> waitingNodes = takeAllWaitingTasksOrFreeLock();
        if (waitingNodes.isEmpty()) {
            return;
        }

        try {
            List<T> commandsInBatch = new ArrayList<>(waitingNodes.size());
            for (WaitingTask<T, R> waitingNode : waitingNodes) {
                commandsInBatch.add(waitingNode.wrappedTask);
            }
            CT multiCommand = taskCombiner.apply(commandsInBatch);
            CompletableFuture<CR> combinedFuture = asyncCombinedTaskExecutor.apply(multiCommand);
            combinedFuture
                    .whenComplete((multiResult, error) -> completeWaitingFutures(waitingNodes, multiResult, error))
                    .whenComplete((multiResult, error) -> scheduleNextBatchAsync());
        } catch (Throwable e) {
            try {
                for (WaitingTask waitingNode : waitingNodes) {
                    waitingNode.future.completeExceptionally(e);
                }
            } finally {
                scheduleNextBatchAsync();
            }
        }
    }

    private void completeWaitingFutures(List<WaitingTask<T, R>> waitingNodes, CR multiResult, Throwable error) {
        if (error != null) {
            for (WaitingTask<T, R> waitingNode : waitingNodes) {
                try {
                    waitingNode.future.completeExceptionally(error);
                } catch (Throwable t) {
                    waitingNode.future.completeExceptionally(t);
                }
            }
        } else {
            List<R> singleResults = combinedResultSplitter.apply(multiResult);
            for (int i = 0; i < waitingNodes.size(); i++) {
                try {
                    waitingNodes.get(i).future.complete(singleResults.get(i));
                } catch (Throwable t) {
                    waitingNodes.get(i).future.completeExceptionally(t);
                }
            }
        }
    }

    private WaitingTask<T, R> lockExclusivelyOrEnqueue(T command) {
        WaitingTask<T, R> waitingTask = new WaitingTask<>(command);

        while (true) {
            WaitingTask<T, R> previous = headReference.get();
            if (previous == QUEUE_EMPTY) {
                if (headReference.compareAndSet(previous, QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS)) {
                    return null;
                } else {
                    continue;
                }
            }

            waitingTask.previous = previous;
            if (headReference.compareAndSet(previous, waitingTask)) {
                return waitingTask;
            } else {
                waitingTask.previous = null;
            }
        }
    }

    private List<WaitingTask<T, R>> takeAllWaitingTasksOrFreeLock() {
        WaitingTask<T, R> head;
        while (true) {
            head = headReference.get();
            if (head == QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS) {
                if (headReference.compareAndSet(QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS, QUEUE_EMPTY)) {
                    return Collections.emptyList();
                } else {
                    continue;
                }
            }

            if (headReference.compareAndSet(head, QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS)) {
                break;
            }
        }

        WaitingTask<T, R> current = head;
        List<WaitingTask<T, R>> waitingNodes = new ArrayList<>();
        while (current != QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS) {
            waitingNodes.add(current);
            WaitingTask<T, R> tmp = current.previous;
            current.previous = null; // nullify the reference to previous node in order to avoid GC nepotism
            current = tmp;
        }
        Collections.reverse(waitingNodes);
        return waitingNodes;
    }

    private static class WaitingTask<T, R> {

        public final T wrappedTask;
        public final CompletableFuture<R> future = new CompletableFuture<>();

        public WaitingTask<T, R> previous;

        WaitingTask(T task) {
            this.wrappedTask = task;
        }

    }

}
