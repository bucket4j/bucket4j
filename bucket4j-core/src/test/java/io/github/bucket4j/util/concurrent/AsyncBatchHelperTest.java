package io.github.bucket4j.util.concurrent;

import io.github.bucket4j.distributed.proxy.optimization.batch.mock.MockBatchExecutor;
import io.github.bucket4j.distributed.proxy.optimization.batch.mock.SingleMockCommand;
import io.github.bucket4j.util.concurrent.batch.AsyncBatchHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsyncBatchHelperTest {

    MockBatchExecutor executor = new MockBatchExecutor();

    @Timeout(10)
    @Test
    void testSuccessAsyncCase() throws Exception {
        assertThat(executor.getAsyncBatchHelper().executeAsync(new SingleMockCommand(4)).get()).isEqualTo(4L);
        assertThat(executor.getAsyncBatchHelper().executeAsync(new SingleMockCommand(6)).get()).isEqualTo(10L);
        assertThat(executor.getAsyncBatchHelper().executeAsync(new SingleMockCommand(5)).get()).isEqualTo(15L);
    }

    @Timeout(10)
    @Test
    void testAsyncBatching() throws Exception {
        // setup:
        SingleMockCommand cmd1 = new SingleMockCommand(1, true);
        SingleMockCommand cmd2 = new SingleMockCommand(1, true);
        SingleMockCommand cmd3 = new SingleMockCommand(1, true);
        SingleMockCommand cmd4 = new SingleMockCommand(1, true);

        // when:
        CompletableFuture<Long> future1 = executor.getAsyncBatchHelper().executeAsync(cmd1);
        cmd1.arriveSignal.await();
        CompletableFuture<Long> future2 = executor.getAsyncBatchHelper().executeAsync(cmd2);
        CompletableFuture<Long> future3 = executor.getAsyncBatchHelper().executeAsync(cmd3);
        // then:
        assertThat(cmd2.arriveSignal.getCount()).isEqualTo(1L);
        assertThat(cmd3.arriveSignal.getCount()).isEqualTo(1L);

        // when:
        cmd1.executePermit.countDown();
        cmd2.arriveSignal.await();
        CompletableFuture<Long> future4 = executor.getAsyncBatchHelper().executeAsync(cmd4);
        // then:
        assertThat(future1.get()).isEqualTo(1L);
        assertThat(cmd4.arriveSignal.getCount()).isEqualTo(1L);

        // when:
        cmd2.executePermit.countDown();
        cmd3.arriveSignal.await();
        // then:
        assertThat(cmd4.arriveSignal.getCount()).isEqualTo(1L);

        // when:
        cmd3.executePermit.countDown();
        cmd4.arriveSignal.await();
        // then:
        assertThat(future2.get()).isEqualTo(2L);
        assertThat(future3.get()).isEqualTo(3L);

        // when:
        cmd4.executePermit.countDown();
        // then:
        assertThat(future4.get()).isEqualTo(4L);
    }

    @Timeout(10)
    @Test
    void testThatFailOfSingleTaskDoesNotPreventToStartNextBatchInAsyncExecution() throws Exception {
        // setup:
        SingleMockCommand cmd1 = new SingleMockCommand(new IllegalStateException(), true);
        SingleMockCommand cmd2 = new SingleMockCommand(1, true);
        SingleMockCommand cmd3 = new SingleMockCommand(1, true);

        // when:
        CompletableFuture<Long> future1 = executor.getAsyncBatchHelper().executeAsync(cmd1);
        cmd1.arriveSignal.await();
        CompletableFuture<Long> future2 = executor.getAsyncBatchHelper().executeAsync(cmd2);
        CompletableFuture<Long> future3 = executor.getAsyncBatchHelper().executeAsync(cmd3);
        cmd1.executePermit.countDown();
        cmd2.arriveSignal.await();
        // then:
        assertThatThrownBy(() -> future1.get()).isInstanceOf(ExecutionException.class);

        // when:
        cmd2.executePermit.countDown();
        cmd3.arriveSignal.await();
        cmd3.executePermit.countDown();
        // then:
        assertThat(future2.get()).isEqualTo(1L);
        assertThat(future3.get()).isEqualTo(2L);
    }

    @Timeout(10)
    @Test
    void testThatFailOfBatchOfTasksDoesNotPreventToStartNextBatchInAsyncExecution() throws Exception {
        // setup:
        SingleMockCommand cmd1 = new SingleMockCommand(1, true);
        SingleMockCommand cmd2 = new SingleMockCommand(new IllegalStateException(), true);
        SingleMockCommand cmd3 = new SingleMockCommand(1, true);
        SingleMockCommand cmd4 = new SingleMockCommand(1, true);

        // when:
        CompletableFuture<Long> future1 = executor.getAsyncBatchHelper().executeAsync(cmd1);
        cmd1.arriveSignal.await();
        CompletableFuture<Long> future2 = executor.getAsyncBatchHelper().executeAsync(cmd2);
        CompletableFuture<Long> future3 = executor.getAsyncBatchHelper().executeAsync(cmd3);
        cmd1.executePermit.countDown();
        cmd2.arriveSignal.await();
        CompletableFuture<Long> future4 = executor.getAsyncBatchHelper().executeAsync(cmd4);
        // then:
        assertThat(future1.get()).isEqualTo(1L);

        // when:
        cmd2.executePermit.countDown();
        cmd3.executePermit.countDown();
        cmd4.arriveSignal.await();
        // then:
        assertThat(isCompletedExceptionally(future2)).isTrue();
        assertThat(isCompletedExceptionally(future3)).isTrue();

        // when:
        cmd4.executePermit.countDown();
        // then:
        assertThat(future4.get()).isEqualTo(2L);
    }

    /**
     * Reproduces a bug where a synchronous (as opposed to a failed-future) exception thrown by the
     * solo-execution path of {@link AsyncBatchHelper#executeAsync} left the internal lock permanently
     * acquired, hanging every task enqueued afterwards. Real backends (e.g. a {@code ProxyManager}
     * implementation) can throw synchronously instead of always returning a failed future, so this is
     * reachable in production, not just a theoretical corner case.
     */
    @Timeout(10)
    @Test
    void testThatSynchronousExceptionInSoloExecutionDoesNotHangSubsequentTasks() throws Exception {
        // setup:
        RuntimeException poison = new IllegalStateException("synchronous failure from backend");
        AsyncBatchHelper<Long, Long, List<Long>, List<Long>> helper = AsyncBatchHelper.create(
                tasks -> tasks,
                tasks -> {
                    if (tasks.contains(-1L)) {
                        // simulates a backend that throws synchronously instead of failing the future
                        throw poison;
                    }
                    return CompletableFuture.completedFuture(tasks);
                },
                (combinedTask, combinedResult) -> combinedResult
        );

        // when: the very first call on a fresh helper hits the solo-execution path and fails synchronously
        CompletableFuture<Long> failed = helper.executeAsync(-1L);
        // then:
        assertThatThrownBy(failed::get).isInstanceOf(ExecutionException.class).hasCause(poison);

        // when: a subsequent, unrelated task is submitted
        CompletableFuture<Long> next = helper.executeAsync(42L);
        // then: it must complete and not hang forever waiting for a lock that was never released
        assertThat(next.get()).isEqualTo(42L);
    }

    /**
     * Reproduces a bug where {@code combinedResultSplitter} throwing after a *successful* combined
     * execution left every waiting task in that batch without its future ever being completed (neither
     * normally nor exceptionally), hanging their callers forever. The lock itself was released (so
     * unrelated later tasks were not affected), but everyone waiting on the poisoned batch was stuck.
     */
    @Timeout(10)
    @Test
    void testThatResultSplitterExceptionOnSuccessfulBatchDoesNotHangOtherWaitingTasks() throws Exception {
        // setup:
        RuntimeException splitterError = new IllegalStateException("bug while unwrapping combined result");
        CountDownLatch firstArrived = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AsyncBatchHelper<Long, Long, List<Long>, List<Long>> helper = AsyncBatchHelper.create(
                tasks -> tasks,
                tasks -> CompletableFuture.supplyAsync(() -> {
                    firstArrived.countDown();
                    try {
                        releaseFirst.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return tasks;
                }),
                (combinedTask, combinedResult) -> {
                    if (combinedTask.size() > 1) {
                        // splitter fails even though the combined call itself succeeded
                        throw splitterError;
                    }
                    return combinedResult;
                }
        );

        // when: the first call takes the solo path and blocks inside the executor
        CompletableFuture<Long> first = helper.executeAsync(1L);
        firstArrived.await();

        // and: these two get queued together while "first" is in flight, so they will be
        // drained and executed as a single multi-item batch once "first" completes
        CompletableFuture<Long> second = helper.executeAsync(2L);
        CompletableFuture<Long> third = helper.executeAsync(3L);
        releaseFirst.countDown();

        // then:
        assertThat(first.get()).isEqualTo(1L);
        assertThatThrownBy(second::get).isInstanceOf(ExecutionException.class).hasCause(splitterError);
        assertThatThrownBy(third::get).isInstanceOf(ExecutionException.class).hasCause(splitterError);
    }

    CompletableFuture<Long> runBlockingInNewThread(SingleMockCommand cmd) throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CompletableFuture<Long> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                startLatch.countDown();
                Long result = executor.getSyncBatchHelper().execute(cmd);
                future.complete(result);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }).start();
        startLatch.await();
        Thread.sleep(1000);
        return future;
    }

    boolean isCompletedExceptionally(CompletableFuture<Long> future) {
        try {
            future.get();
            return false;
        } catch (Throwable t) {
            return true;
        }
    }

}
