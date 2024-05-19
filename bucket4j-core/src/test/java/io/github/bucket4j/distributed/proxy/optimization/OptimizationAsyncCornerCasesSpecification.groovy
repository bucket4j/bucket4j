package io.github.bucket4j.distributed.proxy.optimization

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.optimization.delay.DelayOptimization
import io.github.bucket4j.distributed.proxy.optimization.manual.ManuallySyncingOptimization
import io.github.bucket4j.distributed.proxy.optimization.predictive.PredictiveOptimization
import io.github.bucket4j.distributed.proxy.optimization.skiponzero.SkipSyncOnZeroOptimization
import io.github.bucket4j.mock.*
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture

class OptimizationAsyncCornerCasesSpecification extends Specification {

    private static final DelayParameters delayParameters = new DelayParameters(1, Duration.ofNanos(1));

    @Shared
    private TimeMeterMock clock = new TimeMeterMock()


    // https://github.com/bucket4j/bucket4j/issues/398
    @Unroll
    def "should correctly handle exceptions when optimization is used #testNumber ProxyManagerMock"(int testNumber, Optimization optimization) {
        setup:
            ProxyManagerMock proxyManagerMock = new ProxyManagerMock(clock)
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            AsyncBucketProxy bucket = proxyManagerMock.asAsync().builder()
                    .withOptimization(optimization)
                    .build("66", {CompletableFuture.completedFuture(configuration)})
        when:
            bucket.getAvailableTokens().get() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1).get()
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90).get() == null
            bucket.getAvailableTokens().get() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90).get()
            bucket.asVerbose().getAvailableTokens().get().getValue() == 100

        where:
            [testNumber, optimization] << [
                [1, Optimizations.batching()],
                [2, new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [3, new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock)],
                [5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)]
        ]
    }

    // https://github.com/bucket4j/bucket4j/issues/398
    @Unroll
    def "should correctly handle exceptions when optimization is used #testNumber CompareAndSwapBasedProxyManagerMock"(int testNumber, Optimization optimization) {
        setup:
            CompareAndSwapBasedProxyManagerMock proxyManagerMock = new CompareAndSwapBasedProxyManagerMock(ClientSideConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            AsyncBucketProxy bucket = proxyManagerMock.asAsync().builder()
                    .withOptimization(optimization)
                    .build("66", () -> CompletableFuture.completedFuture(configuration))
        when:
            bucket.getAvailableTokens().get() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1).get()
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90).get() == null
            bucket.getAvailableTokens().get() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90).get()
            bucket.asVerbose().getAvailableTokens().get().getValue() == 100

        where:
        [testNumber, optimization] << [
                [1, Optimizations.batching()],
                [2, new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [3, new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock)],
                [5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)]
        ]
    }

}
