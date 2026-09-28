package io.github.bucket4j.util.concurrent;

import io.github.bucket4j.distributed.proxy.optimization.batch.mock.MockBatchExecutor;
import io.github.bucket4j.distributed.proxy.optimization.batch.mock.SingleMockCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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
