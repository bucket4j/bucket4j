package io.github.bucket4j.util.concurrent;

import io.github.bucket4j.util.concurrent.batch.AsyncBatchHelper;
import io.github.bucket4j.util.concurrent.batch.MultiAsyncBatcherHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    /**
     * Reproduces a gap in the previous test suite: {@code batcherIsRemovedFromRegistryAfterFailure} only
     * exercises an asynchronous failure (a failed future returned normally). It never exercised the
     * {@code catch (Throwable t)} branch of {@link MultiAsyncBatcherHelper#executeAsync}, which is reached
     * only when the batcher itself throws synchronously instead of returning a (possibly failed) future.
     * A misbehaving factory returning {@code null} is the only way to trigger that branch through the
     * public API, since {@link AsyncBatchHelper} itself can no longer throw synchronously. Without that
     * catch branch releasing the registry entry, the key would be stuck forever with a non-zero in-flight
     * count, and every subsequent caller for the same key would silently reuse the broken entry instead of
     * getting a fresh batcher.
     */
    @Timeout(10)
    @Test
    void synchronousFailureFromBatcherStillReleasesRegistryEntry() throws Exception {
        assertThatThrownBy(() -> helper.executeAsync("key", 1, key -> null))
            .isInstanceOf(NullPointerException.class);

        // a new batcher must be created for the key, proving the entry was released despite the synchronous failure
        assertThat(helper.executeAsync("key", 2, key -> newCountingBatcher()).get()).isEqualTo(2);
        assertThat(createdBatchersCount.get()).isEqualTo(1);
    }

    /**
     * Verifies that an exception thrown directly by {@code batcherFactory} while creating a brand-new entry
     * (as opposed to one thrown by the batcher itself) propagates to the caller and leaves no entry behind
     * in the registry - relying on {@code ConcurrentHashMap.compute}'s guarantee that the map is left
     * unmodified if the remapping function throws.
     */
    @Timeout(10)
    @Test
    void batcherFactoryExceptionLeavesNoEntryInRegistry() throws Exception {
        RuntimeException factoryError = new IllegalStateException("factory boom");

        assertThatThrownBy(() -> helper.executeAsync("key", 1, key -> {
            throw factoryError;
        })).isSameAs(factoryError);

        // a working factory must be tried for the next call, proving no broken entry was left in the registry
        assertThat(helper.executeAsync("key", 2, key -> newCountingBatcher()).get()).isEqualTo(2);
        assertThat(createdBatchersCount.get()).isEqualTo(1);
    }

    /**
     * Stresses the same key with many real concurrent threads (as opposed to the deterministic,
     * latch-controlled two-caller scenarios above) to gain confidence that the
     * {@code ConcurrentHashMap.compute}-based in-progress refcounting never corrupts under genuine
     * scheduling races: every task must complete (the surrounding {@link Timeout} would catch a hang),
     * and once the whole burst has drained, the registry entry for the key must have been evicted rather
     * than left dangling with a stuck non-zero count.
     */
    @Timeout(20)
    @Test
    void concurrentRequestsForSameKeyAreCorrectlyAccountedUnderRealConcurrency() throws Exception {
        int threadCount = 20;
        int tasksPerThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        try {
            CountDownLatch ready = new CountDownLatch(1);
            List<CompletableFuture<Integer>> futures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                for (int i = 0; i < tasksPerThread; i++) {
                    int value = t * tasksPerThread + i;
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        awaitUninterruptibly(ready);
                        return helper.executeAsync("key", value, key -> newCountingBatcher());
                    }, pool).thenCompose(Function.identity()));
                }
            }
            ready.countDown();

            List<Integer> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
            assertThat(results).containsExactlyInAnyOrderElementsOf(
                    IntStream.range(0, threadCount * tasksPerThread).boxed().collect(Collectors.toList()));
        } finally {
            pool.shutdown();
        }

        // if the registry entry for "key" were stuck with a non-zero in-progress count, this call would
        // silently reuse the stale (already idle) batcher without ever invoking the factory again
        int createdBefore = createdBatchersCount.get();
        assertThat(helper.executeAsync("key", -1, key -> newCountingBatcher()).get()).isEqualTo(-1);
        assertThat(createdBatchersCount.get()).isGreaterThan(createdBefore);
    }

    /**
     * Verifies the class's core documented promise - "created lazily via the supplied factory on first
     * use, shared by all concurrent callers targeting the same key" - by racing many real threads against
     * a brand-new key and asserting the factory was invoked exactly once, relying on
     * {@code ConcurrentHashMap.compute}'s per-key atomicity.
     */
    @Timeout(10)
    @Test
    void concurrentRequestsForNewKeyShareExactlyOneBatcherInstance() throws Exception {
        int threadCount = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch arrived = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        AtomicInteger factoryInvocations = new AtomicInteger();

        try {
            List<CompletableFuture<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                int value = i;
                futures.add(CompletableFuture.supplyAsync(() -> {
                    awaitUninterruptibly(ready);
                    return helper.executeAsync("new-key", value, key -> {
                        factoryInvocations.incrementAndGet();
                        return AsyncBatchHelper.create(
                            tasks -> tasks,
                            tasks -> {
                                arrived.countDown();
                                return awaitAndComplete(proceed, tasks);
                            },
                            (tasks, results) -> results
                        );
                    });
                }, pool).thenCompose(Function.identity()));
            }

            ready.countDown();
            arrived.await();
            // give every racing thread a chance to reach (and be queued behind) the compute() call
            // before letting the in-flight solo execution proceed
            Thread.sleep(200);
            proceed.countDown();

            List<Integer> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
            assertThat(results).containsExactlyInAnyOrderElementsOf(
                    IntStream.range(0, threadCount).boxed().collect(Collectors.toList()));
            assertThat(factoryInvocations.get()).isEqualTo(1);
        } finally {
            pool.shutdown();
        }
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
