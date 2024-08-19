package io.github.bucket4j.distributed.proxy.synchronization.delay


import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.DefaultSynchronizationListener
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.DelayParameters
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronization
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.delay.DelayBucketSynchronization
import io.github.bucket4j.mock.ProxyManagerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CompletableFuture

class DelayedAsyncCommandExecutorSpecification extends Specification {

    private TimeMeterMock clock = new TimeMeterMock()
    private ProxyManagerMock proxyManager = new ProxyManagerMock(clock)
    private DefaultSynchronizationListener listener = new DefaultSynchronizationListener();
    private BucketConfiguration configuration = BucketConfiguration.builder()
        .addLimit({it.capacity(100).refillGreedy(100, Duration.ofMillis(1000))})
        .build()
    private DelayParameters parameters = new DelayParameters(20, Duration.ofMillis(500))
    private BucketSynchronization synchronization = new DelayBucketSynchronization(parameters, listener, clock)
    private AsyncBucketProxy optimizedBucket = proxyManager.asAsync().builder()
        .withSynchronization(synchronization)
        .build(1L, () -> CompletableFuture.completedFuture(configuration))
    private Bucket notOptimizedBucket = proxyManager.builder()
        .build(1L, () -> configuration)

    def "Should delay async consumption"() {
        when: "first tryAcquire(1) happened"
            boolean consumed = optimizedBucket.tryConsume(1).get()
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens().get() == 99
        and: "request propagated to proxyManager"
            notOptimizedBucket.getAvailableTokens() == 99
        and: "metrics correctly counted"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 1 // getAvailableTokens increments this counter

        when: "next tryAcquire(1) happened after 9 millis"
            clock.addMillis(9) // 9
            consumed = optimizedBucket.tryConsume(1).get()
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens().get() == 98
        and: "request not propagated to proxyManager because"
            notOptimizedBucket.getAvailableTokens() == 99
        and: "metrics correctly counted"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 3

        when: "next tryAcquire(1) happened after 1 millis"
            clock.addMillis(1) // 10
            consumed = optimizedBucket.tryConsume(1).get()
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens().get() == 98 // one token was refilled
        and: "request not propagated to proxyManager"
            notOptimizedBucket.getAvailableTokens() == 100 // one token was refilled
        and: "metrics correctly counted"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 5

        when: "next tryAcquire(19) happened after 10 millis"
            clock.addMillis(10) // 20
            consumed = optimizedBucket.tryConsume(19).get()
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens().get() == 79
        and: "request propagated to proxyManager because of overflow of delay threshold"
            notOptimizedBucket.getAvailableTokens() == 79 // one token was refilled
        and: "metrics correctly counted"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 6

        when: "all tokens consumed from remote bucket"
            notOptimizedBucket.tryConsumeAsMuchAsPossible()
        then: "it is possible to consume from local bucket because sync timeout is not exceeded"
            optimizedBucket.tryConsume(1).get() == true
            optimizedBucket.getAvailableTokens().get() == 78
            optimizedBucket.tryConsumeAsMuchAsPossible(15).get() == 15
            optimizedBucket.getAvailableTokens().get() == 63

        when: "500 millis passed"
            clock.addMillis(500) // 500
        then: "request not propogated to proxyManager"
            optimizedBucket.getAvailableTokens().get() == 100
            notOptimizedBucket.getAvailableTokens() == 50

        when: "1 millis passed"
            clock.addMillis(1) // 501
        then: "request propogated to proxyManager"
            optimizedBucket.getAvailableTokens().get() == 34
            notOptimizedBucket.getAvailableTokens() == 34

        when: "too many optimized bucket overconsumed the bucket"
            List<Bucket> buckets = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                buckets.add(proxyManager.asAsync().builder().withSynchronization(synchronization).build(1L, {CompletableFuture.completedFuture(configuration)}))
            }
            for (int i = 0; i < 10; i++) {
                buckets.get(i).getAvailableTokens() // just request needed to sync bucket with proxyManager
                buckets.get(i).tryConsume(20);
            }
        then: "amount of token in the proxyManager become negative"
            for (int i = 0; i < 10; i++) {
                buckets.get(i).tryConsume(1) == false;
            }
            notOptimizedBucket.getAvailableTokens() == -167
    }

    def "test synchronization by requirement"() {
        when: "one token consumed without synchronization"
            optimizedBucket.getAvailableTokens().get()
            optimizedBucket.tryConsume(1).get()
        then:
            optimizedBucket.getAvailableTokens().get() == 99
            notOptimizedBucket.getAvailableTokens() == 100

        when: "explicit synchronization request"
            optimizedBucket.getSynchronizationController().syncImmediately().get()
        then: "synchronization performed"
            optimizedBucket.getAvailableTokens().get() == 99
            notOptimizedBucket.getAvailableTokens() == 99
    }

    def "test synchronization by conditional requirement"() {
        when: "10 tokens consumed without synchronization"
            optimizedBucket.getAvailableTokens().get()
            optimizedBucket.tryConsume(10).get()
        then:
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 100

        when: "synchronization requested with thresholds 20 tokens"
            optimizedBucket.getSynchronizationController().syncByCondition(20, Duration.ZERO).get()
        then: "synchronization have not performed"
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 100

        when: "synchronization requested with thresholds 10 tokens"
            optimizedBucket.getSynchronizationController().syncByCondition(10, Duration.ZERO).get()
        then: "synchronization have not performed"
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 90

        when: "synchronization requested with thresholds 10 tokens"
            optimizedBucket.getSynchronizationController().syncByCondition(10, Duration.ZERO).get()
        then: "synchronization have performed"
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 90

        when: "synchronization requested with thresholds 10 tokens"
            optimizedBucket.getSynchronizationController().syncByCondition(10, Duration.ZERO).get()
        then: "synchronization have performed"
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 90

        when: "9 millis passed 10 tokens consumed and synchronization requested with 20 millis limit"
            clock.addMillis(9)
            optimizedBucket.tryConsume(10).get()
            optimizedBucket.getSynchronizationController().syncByCondition(10, Duration.ofMillis(20)).get()
        then: "synchronization have not performed"
            optimizedBucket.getAvailableTokens().get() == 80
            notOptimizedBucket.getAvailableTokens() == 90

        when: "synchronization requested with limit 10 millis + 9 tokens"
            optimizedBucket.getSynchronizationController().syncByCondition(9, Duration.ofMillis(10)).get()
        then: "synchronization have not performed"
            optimizedBucket.getAvailableTokens().get() == 80
            notOptimizedBucket.getAvailableTokens() == 90

        when: "synchronization requested with limit 9 millis + 10 tokens"
            optimizedBucket.getSynchronizationController().syncByCondition(10, Duration.ofMillis(9)).get()
        then: "synchronization have performed"
            optimizedBucket.getAvailableTokens().get() == 80
            notOptimizedBucket.getAvailableTokens() == 80
    }

}
