package io.github.bucket4j.distributed.proxy.optimization

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.BucketProxy
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.optimization.delay.DelayOptimization
import io.github.bucket4j.distributed.proxy.optimization.manual.ManuallySyncingOptimization
import io.github.bucket4j.distributed.proxy.optimization.predictive.PredictiveOptimization
import io.github.bucket4j.distributed.proxy.optimization.skiponzero.SkipSyncOnZeroOptimization
import io.github.bucket4j.mock.CompareAndSwapBasedProxyManagerMock
import io.github.bucket4j.mock.LockBasedProxyManagerMock
import io.github.bucket4j.mock.ProxyManagerMock
import io.github.bucket4j.mock.SelectForUpdateBasedProxyManagerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS
import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION

class OptimizationCornerCases extends Specification {

    private static final DelayParameters delayParameters = new DelayParameters(1, Duration.ofNanos(1));

    @Shared
    private TimeMeterMock clock = new TimeMeterMock()


    // https://github.com/bucket4j/bucket4j/issues/398
    @Unroll
    def "should correctly handle exceptions when optimization is used #testNumber ProxyManagerMock"(int testNumber, Optimization optimization) {
        setup:
            ProxyManagerMock proxyManagerMock = new ProxyManagerMock(clock)
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofSeconds (1)))
                    .build()

            BucketProxy bucket = proxyManagerMock.builder()
                    .withOptimization(optimization)
                    .build("66", configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

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
                    .addLimit(Bandwidth.simple(10, Duration.ofSeconds (1)))
                    .build()

            BucketProxy bucket = proxyManagerMock.builder()
                    .withOptimization(optimization)
                    .build("66", configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

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
    def "should correctly handle exceptions when optimization is used #testNumber LockBasedProxyManagerMock"(int testNumber, Optimization optimization) {
        setup:
            LockBasedProxyManagerMock proxyManagerMock = new LockBasedProxyManagerMock(ClientSideConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofSeconds (1)))
                    .build()

            BucketProxy bucket = proxyManagerMock.builder()
                    .withOptimization(optimization)
                    .build("66", configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

        where:
            [testNumber, optimization] << [
                    [1, Optimizations.batching()],
                    [2, new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                    [3, new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                    [4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock)],
                    [5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)]
            ]
    }

    @Unroll
    def "should correctly handle exceptions when optimization is used #testNumber SelectForUpdateBasedProxyManagerMock"(int testNumber, Optimization optimization) {
        setup:
            SelectForUpdateBasedProxyManagerMock proxyManagerMock = new SelectForUpdateBasedProxyManagerMock(ClientSideConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofSeconds (1)))
                    .build()

            BucketProxy bucket = proxyManagerMock.builder()
                .withOptimization(optimization)
                .build("66", configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

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
