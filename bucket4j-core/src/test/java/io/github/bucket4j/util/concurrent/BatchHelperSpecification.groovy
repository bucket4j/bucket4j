package io.github.bucket4j.util.concurrent

import io.github.bucket4j.distributed.proxy.optimization.batch.mock.MockBatchExecutor
import io.github.bucket4j.distributed.proxy.optimization.batch.mock.SingleMockCommand
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException

class BatchHelperSpecification extends Specification {

    MockBatchExecutor executor = new MockBatchExecutor()

    @Timeout(10)
    def "test success sync case"() {
        expect:
            executor.getSyncBatchHelper().execute(new SingleMockCommand(4)) == 4
            executor.getSyncBatchHelper().execute(new SingleMockCommand(6)) == 10
            executor.getSyncBatchHelper().execute(new SingleMockCommand(5)) == 15
    }

    @Timeout(10)
    def "test sync batching"() {
        setup:
            def cmd1 = new SingleMockCommand(1, true)
            def cmd2 = new SingleMockCommand(2, true)
            def cmd3 = new SingleMockCommand(3, true)
            def cmd4 = new SingleMockCommand(4, true)

        when:
            CompletableFuture<Long> future1 = runBlockingInNewThread(cmd1);
            cmd1.arriveSignal.await()
            CompletableFuture<Long> future2 = runBlockingInNewThread(cmd2);
            CompletableFuture<Long> future3 = runBlockingInNewThread(cmd3);
        then:
            cmd2.arriveSignal.count == 1
            cmd3.arriveSignal.count == 1

        when:
            cmd1.executePermit.countDown()
            cmd2.arriveSignal.await()
            CompletableFuture<Long> future4 = runBlockingInNewThread(cmd4);
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
            future2.get() == 3L
            future3.get() == 6L

        when:
            cmd4.executePermit.countDown()
        then:
            future4.get() == 10L
    }

    @Timeout(10)
    def "test that fail of single task does not prevent to start next batch in sync execution"() {
        setup:
            def cmd1 = new SingleMockCommand(new IllegalStateException(), true)
            def cmd2 = new SingleMockCommand(1, true)
            def cmd3 = new SingleMockCommand(1, true)

        when:
            CompletableFuture<Long> future1 = runBlockingInNewThread(cmd1);
            cmd1.arriveSignal.await()
            CompletableFuture<Long> future2 = runBlockingInNewThread(cmd2);
            CompletableFuture<Long> future3 = runBlockingInNewThread(cmd3);
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
    def "test that fail of batch of tasks does not prevent to start next batch in sync execution"() {
        setup:
            def cmd1 = new SingleMockCommand(1, true)
            def cmd2 = new SingleMockCommand(new IllegalStateException(), true)
            def cmd3 = new SingleMockCommand(3, true)
            def cmd4 = new SingleMockCommand(4, true)

        when:
            CompletableFuture<Long> future1 = runBlockingInNewThread(cmd1);
            cmd1.arriveSignal.await()
            CompletableFuture<Long> future2 = runBlockingInNewThread(cmd2);
            CompletableFuture<Long> future3 = runBlockingInNewThread(cmd3);
            cmd1.executePermit.countDown()
            cmd2.arriveSignal.await()
            CompletableFuture<Long> future4 = runBlockingInNewThread(cmd4);
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
            future4.get() == 5L
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
