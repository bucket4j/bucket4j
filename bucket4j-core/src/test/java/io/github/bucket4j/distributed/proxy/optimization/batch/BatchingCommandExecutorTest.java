package io.github.bucket4j.distributed.proxy.optimization.batch;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.optimization.DefaultOptimizationListener;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;
import io.github.bucket4j.distributed.proxy.optimization.Optimizations;
import io.github.bucket4j.distributed.remote.MultiResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.remote.commands.MultiCommand;
import io.github.bucket4j.mock.ProxyManagerMock;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("BatchingCommandExecutor Specification")
class BatchingCommandExecutorTest {

    private record VerboseVersionedCase(int n, boolean verbose, boolean versioned) {

        @Override
        public String toString() {
            return "#" + n;
        }
    }

    static Stream<VerboseVersionedCase> verboseVersionedCases() {
        return Stream.of(
            new VerboseVersionedCase(1, false, false),
            new VerboseVersionedCase(2, true, false),
            new VerboseVersionedCase(3, false, true),
            new VerboseVersionedCase(4, true, true)
        );
    }

    private static final TimeMeterMock CLOCK = new TimeMeterMock();
    private static final BucketConfiguration CONFIGURATION = BucketConfiguration.builder()
        .addLimit(limit -> limit.capacity(10_000).refillGreedy(1000, Duration.ofSeconds(1)))
        .build();

    @ParameterizedTest
    @MethodSource("verboseVersionedCases")
    void shouldCombineSequentialTryConsumeRequestsIntoSingleTryConsumeAsMuchAsPossible(VerboseVersionedCase testCase) throws Exception {
        boolean verbose = testCase.verbose();
        boolean versioned = testCase.versioned();

        DefaultOptimizationListener listener = new DefaultOptimizationListener();
        ProxyManagerMock proxyManager = new ProxyManagerMock(CLOCK);
        Bucket bucket = createBucket(versioned, proxyManager, listener);

        bucket.getAvailableTokens();

        proxyManager.blockExecution();

        Callable<Boolean> consumeOneTokenCallable = verbose ?
            () -> bucket.asVerbose().tryConsume(1).getValue() :
            () -> bucket.tryConsume(1);
        CompletableFuture<Boolean> firstResult = runAsync(consumeOneTokenCallable);
        proxyManager.awaitBlockedRequests(1);
        proxyManager.clearHistory();

        List<CompletableFuture<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
            results.add(runAsync(consumeOneTokenCallable));
        }
        Thread.sleep(100);
        proxyManager.unblockExecution();

        for (CompletableFuture<Boolean> feature : results) {
            assertThat(feature.get()).isEqualTo(true);
        }
        List<Request<?>> history = proxyManager.getHistory();
        assertThat(history.size()).isEqualTo(1);
        Request<?> combinedRequest = history.get(0);
        MultiCommand multiCommand = (MultiCommand) combinedRequest.getCommand();
        assertThat(multiCommand.getCommands().size()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("verboseVersionedCases")
    void shouldCombineSequentialTryConsumeRequestsIntoSingleTryConsumeAsMuchAsPossibleWhenNoneMergeableCommandInFirst(VerboseVersionedCase testCase) throws Exception {
        boolean verbose = testCase.verbose();
        boolean versioned = testCase.versioned();

        DefaultOptimizationListener listener = new DefaultOptimizationListener();
        ProxyManagerMock proxyManager = new ProxyManagerMock(CLOCK);
        Bucket bucket = createBucket(versioned, proxyManager, listener);

        bucket.getAvailableTokens();

        proxyManager.blockExecution();

        Callable<Boolean> consumeOneTokenCallable = verbose ?
            () -> bucket.asVerbose().tryConsume(1).getValue() :
            () -> bucket.tryConsume(1);
        Callable<Boolean> consumeTwoTokensCallable = verbose ?
            () -> bucket.asVerbose().tryConsume(2).getValue() :
            () -> bucket.tryConsume(2);
        CompletableFuture<Boolean> firstResult = runAsync(consumeOneTokenCallable);
        proxyManager.awaitBlockedRequests(1);
        proxyManager.clearHistory();

        List<CompletableFuture<Boolean>> results = new ArrayList<>();
        results.add(runAsync(consumeTwoTokensCallable));
        for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
            results.add(runAsync(consumeOneTokenCallable));
        }
        Thread.sleep(100);
        proxyManager.unblockExecution();

        for (CompletableFuture<Boolean> feature : results) {
            assertThat(feature.get()).isEqualTo(true);
        }
        List<Request<?>> history = proxyManager.getHistory();
        assertThat(history.size()).isEqualTo(1);
        Request<?> combinedRequest = history.get(0);
        MultiCommand multiCommand = (MultiCommand) combinedRequest.getCommand();
        assertThat(multiCommand.getCommands().size()).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("verboseVersionedCases")
    void shouldCombineSequentialTryConsumeRequestsIntoSingleTryConsumeAsMuchAsPossibleWhenNoneMergeableCommandInEnd(VerboseVersionedCase testCase) throws Exception {
        boolean verbose = testCase.verbose();
        boolean versioned = testCase.versioned();

        DefaultOptimizationListener listener = new DefaultOptimizationListener();
        ProxyManagerMock proxyManager = new ProxyManagerMock(CLOCK);
        Bucket bucket = createBucket(versioned, proxyManager, listener);

        bucket.getAvailableTokens();

        proxyManager.blockExecution();

        Callable<Boolean> consumeOneTokenCallable = verbose ?
            () -> bucket.asVerbose().tryConsume(1).getValue() :
            () -> bucket.tryConsume(1);
        Callable<Boolean> consumeTwoTokensCallable = verbose ?
            () -> bucket.asVerbose().tryConsume(2).getValue() :
            () -> bucket.tryConsume(2);
        CompletableFuture<Boolean> firstResult = runAsync(consumeOneTokenCallable);
        proxyManager.awaitBlockedRequests(1);
        proxyManager.clearHistory();

        List<CompletableFuture<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
            results.add(runAsync(consumeOneTokenCallable));
        }
        results.add(runAsync(consumeTwoTokensCallable));
        Thread.sleep(100);
        proxyManager.unblockExecution();

        for (CompletableFuture<Boolean> feature : results) {
            assertThat(feature.get()).isEqualTo(true);
        }
        List<Request<?>> history = proxyManager.getHistory();
        assertThat(history.size()).isEqualTo(1);
        Request<?> combinedRequest = history.get(0);
        MultiCommand multiCommand = (MultiCommand) combinedRequest.getCommand();
        assertThat(multiCommand.getCommands().size()).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("verboseVersionedCases")
    void shouldCombineSequentialTryConsumeRequestsIntoSingleTryConsumeAsMuchAsPossibleWhenNoneMergeableCommandInMiddle(VerboseVersionedCase testCase) throws Exception {
        boolean verbose = testCase.verbose();
        boolean versioned = testCase.versioned();

        DefaultOptimizationListener listener = new DefaultOptimizationListener();
        ProxyManagerMock proxyManager = new ProxyManagerMock(CLOCK);
        Bucket bucket = createBucket(versioned, proxyManager, listener);

        bucket.getAvailableTokens();

        proxyManager.blockExecution();

        Callable<Boolean> consumeOneTokenCallable = verbose ?
            () -> bucket.asVerbose().tryConsume(1).getValue() :
            () -> bucket.tryConsume(1);
        Callable<Boolean> consumeTwoTokensCallable = verbose ?
            () -> bucket.asVerbose().tryConsume(2).getValue() :
            () -> bucket.tryConsume(2);
        CompletableFuture<Boolean> firstResult = runAsync(consumeOneTokenCallable);
        proxyManager.awaitBlockedRequests(1);
        proxyManager.clearHistory();

        List<CompletableFuture<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
            if (i == MultiCommand.MERGING_THRESHOLD / 2) {
                results.add(runAsync(consumeTwoTokensCallable));
            } else {
                results.add(runAsync(consumeOneTokenCallable));
            }
        }
        Thread.sleep(100);
        proxyManager.unblockExecution();

        for (CompletableFuture<Boolean> feature : results) {
            assertThat(feature.get()).isEqualTo(true);
        }
        List<Request<?>> history = proxyManager.getHistory();
        assertThat(history.size()).isEqualTo(1);
        Request<?> combinedRequest = history.get(0);
        MultiCommand multiCommand = (MultiCommand) combinedRequest.getCommand();
        assertThat(multiCommand.getCommands().size()).isEqualTo(3);
    }

    @ParameterizedTest
    @MethodSource("verboseVersionedCases")
    void shouldCorrectlyHandleExceptionsWhileUnwrappingFailedResults(VerboseVersionedCase testCase) throws Exception {
        boolean verbose = testCase.verbose();
        boolean versioned = testCase.versioned();

        DefaultOptimizationListener listener = new DefaultOptimizationListener();
        ProxyManagerMock proxyManager = new ProxyManagerMock(CLOCK);
        Bucket bucket = createBucket(versioned, proxyManager, listener);
        bucket.getAvailableTokens();

        proxyManager.blockExecution();

        Callable<Boolean> consumeOneTokenCallable = verbose ?
            () -> bucket.asVerbose().tryConsume(1).getValue() :
            () -> bucket.tryConsume(1);
        Callable<Boolean> consumeTwoTokensCallable = verbose ?
            () -> bucket.asVerbose().tryConsume(2).getValue() :
            () -> bucket.tryConsume(2);
        CompletableFuture<Boolean> firstResult = runAsync(consumeOneTokenCallable);
        proxyManager.awaitBlockedRequests(1);
        proxyManager.clearHistory();

        List<CompletableFuture<Boolean>> results = new ArrayList<>();
        results.add(runAsync(consumeTwoTokensCallable));
        for (int i = 0; i < MultiCommand.MERGING_THRESHOLD; i++) {
            if (i == MultiCommand.MERGING_THRESHOLD / 2) {
                results.add(runAsync(consumeTwoTokensCallable));
            } else {
                results.add(runAsync(consumeOneTokenCallable));
            }
        }
        results.add(runAsync(consumeTwoTokensCallable));
        proxyManager.setException(new RuntimeException("Just because"));
        Thread.sleep(100);
        proxyManager.unblockExecution();
        proxyManager.allowResultReturning();

        for (CompletableFuture<Boolean> feature : results) {
            try {
                feature.get();
                fail();
            } catch (Throwable t) {
                assertThat(feature.isCompletedExceptionally()).isTrue();
            }
        }
        List<Request<?>> history = proxyManager.getHistory();
        assertThat(history.size()).isEqualTo(1);
        Request<?> combinedRequest = history.get(0);
        MultiCommand multiCommand = (MultiCommand) combinedRequest.getCommand();
        assertThat(multiCommand.getCommands().size()).isEqualTo(5);
    }

    @ParameterizedTest
    @MethodSource("verboseVersionedCases")
    void regressionTestForIssue501(VerboseVersionedCase testCase) throws Exception {
        boolean verbose = testCase.verbose();
        boolean versioned = testCase.versioned();

        ProxyManagerMock proxyManager = new ProxyManagerMock(CLOCK);
        Bucket bucket;

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
            // configure always empty bucket
            .addLimit(limit -> limit.capacity(100).refillGreedy(1, Duration.ofDays(1)).initialTokens(0))
            .build();
        if (versioned) {
            bucket = proxyManager.builder()
                .withOptimization(Optimizations.batching())
                .withImplicitConfigurationReplacement(1, TokensInheritanceStrategy.AS_IS)
                .build(42, configSupplier);
        } else {
            bucket = proxyManager.builder()
                .withOptimization(Optimizations.batching())
                .build(42, configSupplier);
        }
        AtomicLong consumedTokens = new AtomicLong();

        int processors = Runtime.getRuntime().availableProcessors();
        CountDownLatch startLatch = new CountDownLatch(processors);
        CountDownLatch stopLatch = new CountDownLatch(processors);
        for (int i = 0; i < processors; i++) {
            new Thread(() -> {
                startLatch.countDown();
                try {
                    startLatch.await();
                    for (int j = 0; j < 1_000; j++) {
                        boolean consumed = verbose ? bucket.asVerbose().tryConsume(1).getValue() : bucket.tryConsume(1);
                        if (consumed) {
                            consumedTokens.addAndGet(1);
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    stopLatch.countDown();
                }
            }).start();
        }
        stopLatch.await();

        assertThat(consumedTokens.get()).isEqualTo(0);
    }

    private <T> CompletableFuture<T> runAsync(Callable<T> callable) throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CompletableFuture<T> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                startLatch.countDown();
                future.complete(callable.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }).start();
        startLatch.await();
        return future;
    }

    private Bucket createBucket(boolean versioned, ProxyManager proxyManager, DefaultOptimizationListener listener) {
        Optimization optimization = new BatchingOptimization(listener);
        if (versioned) {
            return proxyManager.builder()
                .withOptimization(optimization)
                .build(1L, CONFIGURATION);
        } else {
            return proxyManager.builder()
                .withOptimization(optimization)
                .withImplicitConfigurationReplacement(1, TokensInheritanceStrategy.AS_IS)
                .build(1L, CONFIGURATION);
        }
    }

}
