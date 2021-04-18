package io.github.bucket4j.distributed.proxy.optimization.predictive

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.optimization.DefaultOptimizationListener
import io.github.bucket4j.distributed.proxy.optimization.DelayParameters
import io.github.bucket4j.distributed.proxy.optimization.Optimization
import io.github.bucket4j.distributed.proxy.optimization.PredictionParameters
import io.github.bucket4j.mock.ProxyManagerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification

import java.time.Duration

class PredictiveAsyncCommandExecutorSpecification extends Specification {

    private TimeMeterMock clock = new TimeMeterMock()
    private ProxyManagerMock proxyManager = new ProxyManagerMock(clock)
    private DefaultOptimizationListener listener = new DefaultOptimizationListener();
    private BucketConfiguration configuration = BucketConfiguration.builder()
        .addLimit(Bandwidth.simple(100, Duration.ofMillis(1000)))
        .build()
    private DelayParameters delay = new DelayParameters(20, Duration.ofMillis(500))
    private PredictionParameters prediction = PredictionParameters.createDefault(delay)
    private Optimization optimization = new PredictiveOptimization(prediction, delay, listener, clock)
    private AsyncBucketProxy optimizedBucket = proxyManager.asAsync().builder()
        .withOptimization(optimization)
        .buildProxy(1L, configuration)
    private Bucket notOptimizedBucket = proxyManager.builder()
        .buildProxy(1L, configuration)

    def "Should delay sync consumption"() {
        when: "first tryAcquire(1) happened"
            boolean consumed = optimizedBucket.tryConsume(1).get()
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens().get() == 99
        and: "request propagated to proxyManager"
            notOptimizedBucket.getAvailableTokens() == 99
        and: "metrics correctly counted"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 0 // getAvailableTokens creates second sample

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
            listener.getSkipCount() == 2

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
            listener.getSkipCount() == 4

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
            listener.getSkipCount() == 5

        when: "50 tokens consumed from remote bucket"
            notOptimizedBucket.tryConsume(75)
        then: "it is possible to consume from local bucket because sync timeout is not exceeded"
            optimizedBucket.tryConsume(1).get() == true
            optimizedBucket.getAvailableTokens().get() == 78
            optimizedBucket.tryConsumeAsMuchAsPossible(15).get() == 15
            optimizedBucket.getAvailableTokens().get() == 63
            notOptimizedBucket.getAvailableTokens() == 4

        when: "500 millis passed"
            clock.addMillis(500) // 500
        then: "request not propogated to proxyManager"
            optimizedBucket.getAvailableTokens().get() == 100
            notOptimizedBucket.getAvailableTokens() == 54

        when: "1 millis passed"
            clock.addMillis(1) // 501
        then: "request propogated to proxyManager"
            optimizedBucket.getAvailableTokens().get() == 38
            notOptimizedBucket.getAvailableTokens() == 38

        when: "250 millis passed"
            clock.addMillis(250)
        then: "optimized bucket takes care about other nodes consumption rate"
            notOptimizedBucket.getAvailableTokens() == 63
            optimizedBucket.getAvailableTokens().get() == 28

        when: "125 millis passed"
            clock.addMillis(125)
        then: "optimized bucket takes care about other nodes consumption rate"
            notOptimizedBucket.getAvailableTokens() == 75
            optimizedBucket.getAvailableTokens().get() == 22

        when:
            notOptimizedBucket.tryConsumeAsMuchAsPossible()
            optimizedBucket.tryConsume(19).get()

        then: "amount of token in the proxyManager become negative after sync"
            optimizedBucket.getAvailableTokens().get() == 3
            notOptimizedBucket.getAvailableTokens() == 0

        when:
            clock.addMillis(40)
            optimizedBucket.getOptimizationController().syncImmediately().get()
        then:
            optimizedBucket.getAvailableTokens().get() == -15
            notOptimizedBucket.getAvailableTokens() == -15

        when:
            clock.addMillis(100)
        then:
            optimizedBucket.getAvailableTokens().get() == -5
            notOptimizedBucket.getAvailableTokens() == -5

        when:
            clock.addMillis(50)
        then:
            optimizedBucket.getAvailableTokens().get() == 0
            notOptimizedBucket.getAvailableTokens() == 0

        when:
            clock.addMillis(80)
        then:
            optimizedBucket.getAvailableTokens().get() == 0
            notOptimizedBucket.getAvailableTokens() == 8

        when:
            clock.addMillis(20)
        then:
            optimizedBucket.getAvailableTokens().get() == 0
            notOptimizedBucket.getAvailableTokens() == 10

        when:
            clock.addMillis(400)
        then:
            optimizedBucket.getAvailableTokens().get() == 50
            notOptimizedBucket.getAvailableTokens() == 50
    }

    def "test synchronization by requirement"() {
        when: "one token consumed without synchronization"
            optimizedBucket.getAvailableTokens().get()
            clock.addMillis(1);
            optimizedBucket.getAvailableTokens().get()
            optimizedBucket.tryConsume(1)
        then:
            optimizedBucket.getAvailableTokens().get() == 99
            notOptimizedBucket.getAvailableTokens() == 100

        when: "explicit synchronization request"
            optimizedBucket.getOptimizationController().syncImmediately().get()
        then: "synchronization performed"
            optimizedBucket.getAvailableTokens().get() == 99
            notOptimizedBucket.getAvailableTokens() == 99
    }

    def "test synchronization by conditional requirement"() {
        when: "10 tokens consumed without synchronization"
            optimizedBucket.getAvailableTokens().get()
            clock.addTime(1)
            optimizedBucket.getAvailableTokens().get()
            optimizedBucket.tryConsume(10).get()
        then:
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 100

        when: "synchronization requested with thresholds 20 tokens"
            optimizedBucket.getOptimizationController().syncByCondition(20, Duration.ZERO).get()
        then: "synchronization have not performed"
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 100

        when: "synchronization requested with thresholds 10 tokens"
            optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ZERO).get()
        then: "synchronization have not performed"
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 90

        when: "synchronization requested with thresholds 10 tokens"
            optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ZERO).get()
        then: "synchronization have performed"
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 90

        when: "synchronization requested with thresholds 10 tokens"
            optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ZERO).get()
        then: "synchronization have performed"
            optimizedBucket.getAvailableTokens().get() == 90
            notOptimizedBucket.getAvailableTokens() == 90

        when: "9 millis passed 10 tokens consumed and synchronization requested with 20 millis limit"
            clock.addMillis(9)
            optimizedBucket.tryConsume(10).get()
            optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ofMillis(20))
        then: "synchronization have not performed"
            optimizedBucket.getAvailableTokens().get() == 80
            notOptimizedBucket.getAvailableTokens() == 90

        when: "synchronization requested with limit 10 millis + 9 tokens"
            optimizedBucket.getOptimizationController().syncByCondition(9, Duration.ofMillis(10)).get()
        then: "synchronization have not performed"
            optimizedBucket.getAvailableTokens().get() == 80
            notOptimizedBucket.getAvailableTokens() == 90

        when: "synchronization requested with limit 9 millis + 10 tokens"
            optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ofMillis(9)).get()
        then: "synchronization have performed"
            optimizedBucket.getAvailableTokens().get() == 80
            notOptimizedBucket.getAvailableTokens() == 80
    }

}
