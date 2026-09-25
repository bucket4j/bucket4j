package io.github.bucket4j.util.concurrent;

import io.github.bucket4j.util.concurrent.batch.AsyncBatchHelper;
import io.github.bucket4j.util.concurrent.batch.MultiAsyncBatcherHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiAsyncBatcherHelperTest {

    private final MultiAsyncBatcherHelper<String, Integer, Integer, List<Integer>, List<Integer>> helper = new MultiAsyncBatcherHelper<>();
    private final AtomicInteger createdBatchersCount = new AtomicInteger();

    @Timeout(10)
    @Test
    void requestsForSameKeyShareSingleBatcher() throws Exception {
        CountDownLatch arrived = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);

        CompletableFuture<Integer> future1 = helper.executeAsync("key", 1, key -> AsyncBatchHelper.create(
            tasks -> tasks,
            tasks -> {
                arrived.countDown();
                return awaitAndComplete(proceed, tasks);
            },
            (tasks, results) -> results
        ));
        arrived.await();

        CompletableFuture<Integer> future2 = helper.executeAsync("key", 2, key -> {
            throw new AssertionError("batcher must be reused for already known key");
        });

        proceed.countDown();

        assertThat(future1.get()).isEqualTo(1);
        assertThat(future2.get()).isEqualTo(2);
    }

    @Timeout(10)
    @Test
    void differentKeysGetIndependentBatchers() throws Exception {
        assertThat(helper.executeAsync("key1", 1, key -> newCountingBatcher()).get()).isEqualTo(1);
        assertThat(helper.executeAsync("key2", 2, key -> newCountingBatcher()).get()).isEqualTo(2);

        assertThat(createdBatchersCount.get()).isEqualTo(2);
    }

    @Timeout(10)
    @Test
    void batcherIsRecreatedOnceInFlightRequestForKeyCompletes() throws Exception {
        assertThat(helper.executeAsync("key", 1, key -> newCountingBatcher()).get()).isEqualTo(1);
        assertThat(helper.executeAsync("key", 2, key -> newCountingBatcher()).get()).isEqualTo(2);

        assertThat(createdBatchersCount.get()).isEqualTo(2);
    }

    @Timeout(10)
    @Test
    void batcherIsRemovedFromRegistryAfterFailure() throws Exception {
        CompletableFuture<Integer> failedFuture = helper.executeAsync("key", 1, key -> AsyncBatchHelper.create(
            tasks -> tasks,
            tasks -> CompletableFuture.failedFuture(new IllegalStateException()),
            (tasks, results) -> results
        ));
        assertThatThrownBy(failedFuture::get).hasCauseInstanceOf(IllegalStateException.class);

        // a new batcher must be created for the key, proving the failed entry was evicted from the registry
        assertThat(helper.executeAsync("key", 2, key -> newCountingBatcher()).get()).isEqualTo(2);
        assertThat(createdBatchersCount.get()).isEqualTo(1);
    }

    private AsyncBatchHelper<Integer, Integer, List<Integer>, List<Integer>> newCountingBatcher() {
        createdBatchersCount.incrementAndGet();
        return AsyncBatchHelper.create(
            tasks -> tasks,
            CompletableFuture::completedFuture,
            (tasks, results) -> results
        );
    }

    private CompletableFuture<List<Integer>> awaitAndComplete(CountDownLatch proceed, List<Integer> tasks) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                proceed.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return tasks;
        });
    }

}
