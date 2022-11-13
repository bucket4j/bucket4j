/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2022 Vladimir Bukhtoyarov
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.bucket4j.util.concurrent.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
public class BatchHelper<T, R, CT, CR> {

    private static final Object NEED_TO_EXECUTE_NEXT_BATCH = new Object();
    private static final WaitingTask<?, ?> QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS = new WaitingTask<>(null);
    private static final WaitingTask<?, ?> QUEUE_EMPTY = new WaitingTask<>(null);

    private final Function<List<T>, CT> taskCombiner;
    private final Function<CT, CR> combinedTaskExecutor;
    private final Function<T, R> taskExecutor;

    private final Function<CR, List<R>> combinedResultSplitter;

    private final AtomicReference<WaitingTask> headReference = new AtomicReference<>(QUEUE_EMPTY);

    public static <T, R, CT, CR> BatchHelper<T, R, CT, CR> create(
            Function<List<T>, CT> taskCombiner,
            Function<CT, CR> combinedTaskExecutor,
            Function<T, R> taskExecutor,
            Function<CR, List<R>> combinedResultSplitter) {
        return new BatchHelper<>(taskCombiner, combinedTaskExecutor, taskExecutor, combinedResultSplitter);
    }

    public static <T, R, CT, CR> BatchHelper<T, R, CT, CR> create(
            Function<List<T>, CT> taskCombiner,
            Function<CT, CR> combinedTaskExecutor,
            Function<CR, List<R>> combinedResultSplitter) {
        Function<T, R> taskExecutor = new Function<T, R>() {
            @Override
            public R apply(T task) {
                CT combinedTask = taskCombiner.apply(Collections.singletonList(task));
                CR combinedResult = combinedTaskExecutor.apply(combinedTask);
                List<R> results = combinedResultSplitter.apply(combinedResult);
                return results.get(0);
            }
        };
        return new BatchHelper<>(taskCombiner, combinedTaskExecutor, taskExecutor, combinedResultSplitter);
    }

    private BatchHelper(Function<List<T>, CT> taskCombiner,
                        Function<CT, CR> combinedTaskExecutor,
                        Function<T, R> taskExecutor,
                        Function<CR, List<R>> combinedResultSplitter) {
        this.taskCombiner = requireNonNull(taskCombiner);
        this.combinedTaskExecutor = requireNonNull(combinedTaskExecutor);
        this.taskExecutor = requireNonNull(taskExecutor);
        this.combinedResultSplitter = requireNonNull(combinedResultSplitter);
    }

    public R execute(T task) {
        WaitingTask<T, R> waitingNode = lockExclusivelyOrEnqueue(task);

        if (waitingNode == null) {
            try {
                return taskExecutor.apply(task);
            } finally {
                wakeupAnyThreadFromNextBatchOrFreeLock();
            }
        }

        R result = waitingNode.waitUninterruptedly();
        if (result != NEED_TO_EXECUTE_NEXT_BATCH) {
            // our future completed by another thread from current batch
            return result;
        }

        // current thread is responsible to execute the batch of commands
        try {
            return executeBatch(waitingNode);
        } finally {
            wakeupAnyThreadFromNextBatchOrFreeLock();
        }
    }

    private R executeBatch(WaitingTask<T, R> currentWaitingNode) {
        List<WaitingTask<T, R>> waitingNodes = takeAllWaitingTasksOrFreeLock();

        if (waitingNodes.size() == 1) {
            T singleCommand = waitingNodes.get(0).wrappedTask;
            return taskExecutor.apply(singleCommand);
        }

        try {
            int resultIndex = -1;
            List<T> commandsInBatch = new ArrayList<>(waitingNodes.size());
            for (int i = 0; i < waitingNodes.size(); i++) {
                WaitingTask<T, R> waitingNode = waitingNodes.get(i);
                commandsInBatch.add(waitingNode.wrappedTask);
                if (waitingNode == currentWaitingNode) {
                    resultIndex = i;
                }
            }
            CT multiCommand = taskCombiner.apply(commandsInBatch);

            CR multiResult = combinedTaskExecutor.apply(multiCommand);
            List<R> singleResults = combinedResultSplitter.apply(multiResult);
            for (int i = 0; i < waitingNodes.size(); i++) {
                R singleResult = singleResults.get(i);
                waitingNodes.get(i).future.complete(singleResult);
            }

            return singleResults.get(resultIndex);
        } catch (Throwable e) {
            for (WaitingTask<T, R> waitingNode : waitingNodes) {
                waitingNode.future.completeExceptionally(e);
            }
            throw new BatchFailedException(e);
        }
    }

    private WaitingTask<T, R> lockExclusivelyOrEnqueue(T command) {
        WaitingTask<T, R> currentTask = new WaitingTask<>(command);

        while (true) {
            WaitingTask<T, R> previous = headReference.get();
            if (previous == QUEUE_EMPTY) {
                if (headReference.compareAndSet(previous, QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS)) {
                    return null;
                } else {
                    continue;
                }
            }

            currentTask.previous = previous;
            if (headReference.compareAndSet(previous, currentTask)) {
                return currentTask;
            } else {
                currentTask.previous = null;
            }
        }
    }

    private void wakeupAnyThreadFromNextBatchOrFreeLock() {
        while (true) {
            WaitingTask<T, R> previous = headReference.get();
            if (previous == QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS) {
                if (headReference.compareAndSet(QUEUE_EMPTY_BUT_EXECUTION_IN_PROGRESS, QUEUE_EMPTY)) {
                    return;
                } else {
                    continue;
                }
            } else if (previous != QUEUE_EMPTY) {
                previous.future.complete((R) NEED_TO_EXECUTE_NEXT_BATCH);
                return;
            } else {
                // should never come there
                String msg = "Detected illegal usage of API, wakeupAnyThreadFromNextBatchOrFreeLock should not be called on empty queue";
                throw new IllegalStateException(msg);
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
        public final Thread thread = Thread.currentThread();

        public WaitingTask<T, R> previous;

        WaitingTask(T task) {
            this.wrappedTask = task;
        }

        public R waitUninterruptedly() {
            boolean wasInterrupted = false;;
            try {
                while (true) {
                    wasInterrupted = wasInterrupted || Thread.interrupted();
                    try {
                        return future.get();
                    } catch (InterruptedException e) {
                        wasInterrupted = true;
                    } catch (ExecutionException e) {
                        throw new BatchFailedException(e.getCause());
                    }
                }
            } finally {
                if (wasInterrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static class BatchFailedException extends IllegalStateException {

        public BatchFailedException(Throwable e) {
            super(e);
        }

    }

}
