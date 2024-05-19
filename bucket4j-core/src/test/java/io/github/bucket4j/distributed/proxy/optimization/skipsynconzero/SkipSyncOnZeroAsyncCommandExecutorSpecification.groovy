package io.github.bucket4j.distributed.proxy.optimization.skipsynconzero

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.optimization.DefaultOptimizationListener
import io.github.bucket4j.distributed.proxy.optimization.Optimization
import io.github.bucket4j.distributed.proxy.optimization.skiponzero.SkipSyncOnZeroOptimization
import io.github.bucket4j.mock.ProxyManagerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture

class SkipSyncOnZeroAsyncCommandExecutorSpecification extends Specification {

    private TimeMeterMock clock = new TimeMeterMock()
    private ProxyManagerMock proxyManager = new ProxyManagerMock(clock)
    private DefaultOptimizationListener listener = new DefaultOptimizationListener();
    private BucketConfiguration configuration = BucketConfiguration.builder()
        .addLimit({it.capacity(100).refillGreedy(100, Duration.ofMillis(1000))})
        .build()
    private Optimization optimization = new SkipSyncOnZeroOptimization(listener, clock)
    private AsyncBucketProxy optimizedBucket = proxyManager.asAsync().builder()
        .withOptimization(optimization)
        .build(1L, () -> CompletableFuture.completedFuture(configuration));

    def "should skip synchronization with storage when bucket is empty"() {
        when: "bucket becomes empty"
            optimizedBucket.tryConsumeAsMuchAsPossible().get()
        and: "trying to consume again"
            optimizedBucket.tryConsume(1).get()
        then: "request should not be propogated to server"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 1
    }

    def "should correctly calculate the time of next sync with storage with storage"() {
        when: "bucket becomes empty"
            optimizedBucket.tryConsumeAsMuchAsPossible().get()
        and: "trying to consume again"
            optimizedBucket.tryConsume(1).get()
        then: "request should not be propogated to server"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 1

        when: "past enough time to generate single token"
            clock.addMillis(10)
        and: "trying to consume again"
            boolean consumed = optimizedBucket.tryConsume(1).get()
        then: "consumption request should be propogated to storage"
            consumed == true
            listener.getMergeCount() == 0
            listener.getSkipCount() == 1
    }

    def "special commands should lead to immediately synchronization with server"() {
        when: "bucket becomes empty"
            optimizedBucket.tryConsumeAsMuchAsPossible().get()
        and: "reset all limits"
            optimizedBucket.reset()
        then: "request should not be propogated to server"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 0
            optimizedBucket.getAvailableTokens().get() == 100
    }

}