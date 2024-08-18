package io.github.bucket4j.distributed.proxy.optimization.batch

import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.TokensInheritanceStrategy
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.DefaultSynchronizationListener
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronization
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronizations
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch.BatchingBucketSynchronization
import io.github.bucket4j.distributed.remote.MultiResult
import io.github.bucket4j.distributed.remote.Request
import io.github.bucket4j.distributed.remote.commands.MultiCommand
import io.github.bucket4j.mock.ProxyManagerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier

import static org.junit.jupiter.api.Assertions.fail

class BatchingCommandExecutorSpecification extends Specification {

    private static TimeMeterMock clock = new TimeMeterMock()
    private static BucketConfiguration configuration = BucketConfiguration.builder()
        .addLimit({limit -> limit.capacity(10_000).refillGreedy(1000, Duration.ofSeconds(1))})
        .build()

    @Unroll
    def "#n Should combine sequential tryConsume(1) requests into single TryConsumeAsMuchAsPossible verbose=#verbose versioned=#versioned"(int n, boolean verbose, boolean versioned) {
        setup:
            DefaultSynchronizationListener listener = new DefaultSynchronizationListener();
            ProxyManagerMock proxyManager = new ProxyManagerMock(clock)
            Bucket bucket = createBucket(versioned, proxyManager, listener)
        when:
            bucket.getAvailableTokens()

            proxyManager.blockExecution()

            Callable<Boolean> consumeOneTokenCallable = verbose ?
                    { bucket.asVerbose().tryConsume(1).value } as Callable :
                    { bucket.tryConsume(1) } as Callable
            CompletableFuture<Boolean> firstResult =  runAsync(consumeOneTokenCallable)
            proxyManager.awaitBlockedRequests(1)
            proxyManager.clearHistory()

            List<CompletableFuture<Boolean>> results = new ArrayList<>()
            for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
                results.add(runAsync(consumeOneTokenCallable))
            }
            Thread.sleep(100)
            proxyManager.unblockExecution()
        then:
            for (final def feature in results) {
                assert feature.get() == true
            }
            List<Request<?>> history = proxyManager.getHistory()
            history.size() == 1
            Request<MultiResult> combinedRequest = history.get(0)
            MultiCommand multiCommand = combinedRequest.getCommand()
            multiCommand.commands.size() == 1
        where:
            [n, verbose, versioned] << [
                    [1, false, false],
                    [2, true, false],
                    [3, false, true],
                    [4, true, true]
            ]
    }

    @Unroll
    def "#n Should combine sequential tryConsume(1) requests into single TryConsumeAsMuchAsPossible when none-mergeable command in first verbose=#verbose versioned=#versioned"(int n, boolean verbose, boolean versioned) {
        setup:
            DefaultSynchronizationListener listener = new DefaultSynchronizationListener();
            ProxyManagerMock proxyManager = new ProxyManagerMock(clock)
            Bucket bucket = createBucket(versioned, proxyManager, listener)
        when:
            bucket.getAvailableTokens()

            proxyManager.blockExecution()

            def consumeOneTokenCallable = verbose ?
                    { bucket.asVerbose().tryConsume(1).value } as Callable :
                    { bucket.tryConsume(1) } as Callable
            def consumeTwoTokensCallable = verbose ?
                { bucket.asVerbose().tryConsume(2).value } as Callable :
                { bucket.tryConsume(2) } as Callable
            CompletableFuture<Boolean> firstResult =  runAsync(consumeOneTokenCallable)
            proxyManager.awaitBlockedRequests(1)
            proxyManager.clearHistory()

            List<CompletableFuture<Boolean>> results = new ArrayList<>()
            results.add(runAsync(consumeTwoTokensCallable))
            for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
                results.add(runAsync(consumeOneTokenCallable))
            }
            Thread.sleep(100)
            proxyManager.unblockExecution()
        then:
            for (final def feature in results) {
                assert feature.get() == true
            }
            List<Request<?>> history = proxyManager.getHistory()
            history.size() == 1
            Request<MultiResult> combinedRequest = history.get(0)
            MultiCommand multiCommand = combinedRequest.getCommand()
            multiCommand.commands.size() == 2
        where:
            [n, verbose, versioned] << [
                [1, false, false],
                [2, true, false],
                [3, false, true],
                [4, true, true]
            ]
    }

    @Unroll
    def "#n Should combine sequential tryConsume(1) requests into single TryConsumeAsMuchAsPossible  when none-mergeable command in end verbose=#verbose versioned=#versioned"(int n, boolean verbose, boolean versioned) {
        setup:
            DefaultSynchronizationListener listener = new DefaultSynchronizationListener();
            ProxyManagerMock proxyManager = new ProxyManagerMock(clock)
            Bucket bucket = createBucket(versioned, proxyManager, listener)
        when:
            bucket.getAvailableTokens()

            proxyManager.blockExecution()

            def consumeOneTokenCallable = verbose ?
                    { bucket.asVerbose().tryConsume(1).value } as Callable :
                    { bucket.tryConsume(1) } as Callable
            def consumeTwoTokensCallable = verbose ?
                    { bucket.asVerbose().tryConsume(2).value } as Callable :
                    { bucket.tryConsume(2) } as Callable
            CompletableFuture<Boolean> firstResult = runAsync(consumeOneTokenCallable)
            proxyManager.awaitBlockedRequests(1)
            proxyManager.clearHistory()

            List<CompletableFuture<Boolean>> results = new ArrayList<>()
            for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
                results.add(runAsync(consumeOneTokenCallable))
            }
            results.add(runAsync(consumeTwoTokensCallable))
            Thread.sleep(100)
            proxyManager.unblockExecution()
        then:
            for (final def feature in results) {
                assert feature.get() == true
            }
            List<Request<?>> history = proxyManager.getHistory()
            history.size() == 1
            Request<MultiResult> combinedRequest = history.get(0)
            MultiCommand multiCommand = combinedRequest.getCommand()
            multiCommand.commands.size() == 2
        where:
            [n, verbose, versioned] << [
                [1, false, false],
                [2, true, false],
                [3, false, true],
                [4, true, true]
            ]
    }

    @Unroll
    def "#n Should combine sequential tryConsume(1) requests into single TryConsumeAsMuchAsPossible when none-mergeable command in middle verbose=#verbose versioned=#versioned"(int n, boolean verbose, boolean versioned) {
        setup:
            DefaultSynchronizationListener listener = new DefaultSynchronizationListener();
            ProxyManagerMock proxyManager = new ProxyManagerMock(clock)
            Bucket bucket = createBucket(versioned, proxyManager, listener)
        when:
            bucket.getAvailableTokens()

            proxyManager.blockExecution()

            def consumeOneTokenCallable = verbose ?
                    { bucket.asVerbose().tryConsume(1).value } as Callable :
                    { bucket.tryConsume(1) } as Callable
            def consumeTwoTokensCallable = verbose ?
                    { bucket.asVerbose().tryConsume(2).value } as Callable :
                    { bucket.tryConsume(2) } as Callable
            CompletableFuture<Boolean> firstResult =  runAsync(consumeOneTokenCallable)
            proxyManager.awaitBlockedRequests(1)
            proxyManager.clearHistory()

            List<CompletableFuture<Boolean>> results = new ArrayList<>()
            for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
                if (i == (MultiCommand.MERGING_THRESHOLD / 2).intValue()) {
                    results.add(runAsync(consumeTwoTokensCallable))
                } else {
                    results.add(runAsync(consumeOneTokenCallable))
                }
            }
            Thread.sleep(100)
            proxyManager.unblockExecution()

        then:
            for (final def feature in results) {
                assert feature.get() == true
            }
            List<Request<?>> history = proxyManager.getHistory()
            history.size() == 1
            Request<MultiResult> combinedRequest = history.get(0)
            MultiCommand multiCommand = combinedRequest.getCommand()
            multiCommand.commands.size() == 3
        where:
            [n, verbose, versioned] << [
                [1, false, false],
                [2, true, false],
                [3, false, true],
                [4, true, true]
            ]
    }

    @Unroll
    def "#n Should correctly handle exceptions while unwrapping failed results verbose=#verbose versioned=#versioned"(int n, boolean verbose, boolean versioned) {
        when:
            DefaultSynchronizationListener listener = new DefaultSynchronizationListener();
            ProxyManagerMock proxyManager = new ProxyManagerMock(clock)
            Bucket bucket = createBucket(versioned, proxyManager, listener)
            bucket.getAvailableTokens()

            proxyManager.blockExecution()

            Callable<Boolean> consumeOneTokenCallable = verbose ?
                    { bucket.asVerbose().tryConsume(1).value } as Callable :
                    { bucket.tryConsume(1) } as Callable
            Callable<Boolean> consumeTwoTokensCallable = verbose ?
                { bucket.asVerbose().tryConsume(2).value } as Callable :
                { bucket.tryConsume(2) } as Callable
            CompletableFuture<Boolean> firstResult =  runAsync(consumeOneTokenCallable)
            proxyManager.awaitBlockedRequests(1)
            proxyManager.clearHistory()

            List<CompletableFuture<Boolean>> results = new ArrayList<>()
            results.add(runAsync(consumeTwoTokensCallable))
            for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
                if (i == (MultiCommand.MERGING_THRESHOLD / 2).intValue()) {
                    results.add(runAsync(consumeTwoTokensCallable))
                } else {
                    results.add(runAsync(consumeOneTokenCallable))
                }
            }
            results.add(runAsync(consumeTwoTokensCallable))
            proxyManager.setException(new RuntimeException("Just because"))
            Thread.sleep(100)
            proxyManager.unblockExecution()
            proxyManager.allowResultReturning()
        then:
            for (final def feature in results) {
                try {
                    feature.get()
                    fail()
                } catch(Throwable t) {
                    assert feature.isCompletedExceptionally()
                }
            }
            List<Request<?>> history = proxyManager.getHistory()
            history.size() == 1
            Request<MultiResult> combinedRequest = history.get(0)
            MultiCommand multiCommand = combinedRequest.getCommand()
            multiCommand.commands.size() == 5
        where:
            [n, verbose, versioned] << [
                [1, false, false],
                [2, true, false],
                [3, false, true],
                [4, true, true]
            ]
    }

    CompletableFuture<?> runAsync(Callable<?> callable) {
        CountDownLatch startLatch = new CountDownLatch(1)
        CompletableFuture<?> future = new CompletableFuture<>()
        new Thread({
            try {
                startLatch.countDown()
                future.complete(callable.call())
            } catch (Throwable t) {
                future.completeExceptionally(t)
            }
        }).start()
        startLatch.await()
        return future
    }

    Bucket createBucket(boolean versioned, ProxyManager proxyManager, DefaultSynchronizationListener listener) {
        BucketSynchronization optimization = new BatchingBucketSynchronization(listener)
        if (versioned) {
            return proxyManager.builder()
                    .withOptimization(optimization)
                    .build(1L, () -> configuration)
        } else {
            return proxyManager.builder()
                    .withOptimization(optimization)
                    .withImplicitConfigurationReplacement(1, TokensInheritanceStrategy.AS_IS)
                    .build(1L, () -> configuration)
        }
    }

    @Unroll
    def "#n regression test for https://github.com/bucket4j/bucket4j/issues/501"(int n, boolean verbose, boolean versioned) {
        setup:
            ProxyManagerMock proxyManager = new ProxyManagerMock(clock)
            Bucket bucket = null

            Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                // configure always empty bucket
                .addLimit(limit -> limit.capacity(100).refillGreedy(1, Duration.ofDays(1)).initialTokens(0))
                .build()
            if (versioned) {
                bucket = proxyManager.builder()
                    .withOptimization(BucketSynchronizations.batching())
                    .withImplicitConfigurationReplacement(1, TokensInheritanceStrategy.AS_IS)
                    .build(42, configSupplier)
            } else {
                bucket = proxyManager.builder()
                    .withOptimization(BucketSynchronizations.batching())
                    .build(42, configSupplier)
            }
            AtomicLong consumedTokens = new AtomicLong()
        when:
            int processors = Runtime.getRuntime().availableProcessors()
            CountDownLatch startLatch = new CountDownLatch(processors)
            CountDownLatch stopLatch = new CountDownLatch(processors)
            for (int i = 0; i < processors; i++) {
                new Thread(() -> {
                    startLatch.countDown()
                    startLatch.await()
                    try {
                        for (int j = 0; j < 1_000; j++) {
                            boolean consumed = verbose ? bucket.asVerbose().tryConsume(1).value : bucket.tryConsume(1)
                            if (consumed) {
                                consumedTokens.addAndGet(1)
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace()
                    } finally {
                        stopLatch.countDown()
                    }
                }).start()
            }
            stopLatch.await()
        then:
            consumedTokens.get() == 0
        where:
        [n, verbose, versioned] << [
                [1, false, false],
                [2, true, false],
                [3, false, true],
                [4, true, true]
        ]
    }

}
