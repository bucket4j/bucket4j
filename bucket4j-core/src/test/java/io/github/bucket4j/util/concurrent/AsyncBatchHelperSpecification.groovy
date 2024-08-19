package io.github.bucket4j.util.concurrent

import io.github.bucket4j.distributed.proxy.synchronization.batch.mock.MockBatchExecutor
import io.github.bucket4j.distributed.proxy.synchronization.batch.mock.SingleMockCommand
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException

class AsyncBatchHelperSpecification extends Specification {

    MockBatchExecutor executor = new MockBatchExecutor()

    @Timeout(10)
    def "test success async case"() {
        expect:
            executor.getAsyncBatchHelper().executeAsync(new SingleMockCommand(4)).get() == 4
            executor.getAsyncBatchHelper().executeAsync(new SingleMockCommand(6)).get() == 10
            executor.getAsyncBatchHelper().executeAsync(new SingleMockCommand(5)).get() == 15
    }

    @Timeout(10)
    def "test async batching"() {
        setup:
            def cmd1 = new SingleMockCommand(1, true)
            def cmd2 = new SingleMockCommand(1, true)
            def cmd3 = new SingleMockCommand(1, true)
            def cmd4 = new SingleMockCommand(1, true)

        when:
            def future1 = executor.getAsyncBatchHelper().executeAsync(cmd1)
            cmd1.arriveSignal.await()
            def future2 = executor.getAsyncBatchHelper().executeAsync(cmd2)
            def future3 = executor.getAsyncBatchHelper().executeAsync(cmd3)
        then:
            cmd2.arriveSignal.count == 1
            cmd3.arriveSignal.count == 1

        when:
            cmd1.executePermit.countDown()
            cmd2.arriveSignal.await()
            def future4 = executor.getAsyncBatchHelper().executeAsync(cmd4)
        then:
            future1.get() == 1L
            cmd4.arriveSignal.count == 1

        when:
            cmd2.executePermit.countDown()
            cmd3.arriveSignal.await()
        then:
            cmd4.arriveSignal.count == 1

        when:
            cmd3.executePermit.countDown()
            cmd4.arriveSignal.await()
        then:
            future2.get() == 2L
            future3.get() == 3L

        when:
            cmd4.executePermit.countDown()
        then:
            future4.get() == 4L
    }

    @Timeout(10)
    def "test that fail of single task does not prevent to start next batch in async execution"() {
        setup:
            def cmd1 = new SingleMockCommand(new IllegalStateException(), true)
            def cmd2 = new SingleMockCommand(1, true)
            def cmd3 = new SingleMockCommand(1, true)

        when:
            CompletableFuture<Long> future1 = executor.getAsyncBatchHelper().executeAsync(cmd1)
            cmd1.arriveSignal.await()
            CompletableFuture<Long> future2 = executor.getAsyncBatchHelper().executeAsync(cmd2)
            CompletableFuture<Long> future3 = executor.getAsyncBatchHelper().executeAsync(cmd3)
            cmd1.executePermit.countDown()
            cmd2.arriveSignal.await()
            future1.get()
        then:
            thrown(ExecutionException)

        when:
            cmd2.executePermit.countDown()
            cmd3.arriveSignal.await()
            cmd3.executePermit.countDown()
        then:
            future2.get() == 1L
            future3.get() == 2L
    }

    @Timeout(10)
    def "test that fail of batch of tasks does not prevent to start next batch in async execution"() {
        setup:
            def cmd1 = new SingleMockCommand(1, true)
            def cmd2 = new SingleMockCommand(new IllegalStateException(), true)
            def cmd3 = new SingleMockCommand(1, true)
            def cmd4 = new SingleMockCommand(1, true)

        when:
            CompletableFuture<Long> future1 = executor.getAsyncBatchHelper().executeAsync(cmd1)
            cmd1.arriveSignal.await()
            CompletableFuture<Long> future2 = executor.getAsyncBatchHelper().executeAsync(cmd2)
            CompletableFuture<Long> future3 = executor.getAsyncBatchHelper().executeAsync(cmd3)
            cmd1.executePermit.countDown()
            cmd2.arriveSignal.await()
            CompletableFuture<Long> future4 = executor.getAsyncBatchHelper().executeAsync(cmd4)
        then:
            future1.get() == 1L

        when:
            cmd2.executePermit.countDown()
            cmd3.executePermit.countDown()
            cmd4.arriveSignal.await()
        then:
            isCompletedExceptionally(future2)
            isCompletedExceptionally(future3)

        when:
            cmd4.executePermit.countDown()
        then:
            future4.get() == 2L
    }

    CompletableFuture<Long> runBlockingInNewThread(SingleMockCommand cmd) {
        CountDownLatch startLatch = new CountDownLatch(1)
        CompletableFuture<Long> future = new CompletableFuture<>()
        new Thread({
            try {
                startLatch.countDown()
                Long result = executor.getSyncBatchHelper().execute(cmd)
                future.complete(result);
            } catch (Throwable t) {
                future.completeExceptionally(t)
            }
        }).start()
        startLatch.await()
        Thread.sleep(1000)
        return future
    }

    boolean isCompletedExceptionally(CompletableFuture<Long> future) {
        try {
            future.get();
            return false;
        } catch (Throwable t) {
            return true
        }
    }

}
