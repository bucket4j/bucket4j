package io.github.bucket4j.distributed.proxy.optimization.delay

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.optimization.DefaultOptimizationListener
import io.github.bucket4j.distributed.proxy.optimization.DelayParameters
import io.github.bucket4j.distributed.proxy.optimization.Optimization
import io.github.bucket4j.mock.GridBackendMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification

import java.time.Duration

class DelayedCommandExecutorSpecification extends Specification {

    def "Should delay sync consumption"() {
        setup:
            TimeMeterMock clock = new TimeMeterMock()
            GridBackendMock backend = new GridBackendMock(clock)
            DefaultOptimizationListener listener = new DefaultOptimizationListener();
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofMillis(1000)))
                .build()
            DelayParameters parameters = new DelayParameters(20, Duration.ofMillis(500))
            Optimization optimization = new DelayOptimization(parameters, listener, clock)
            Bucket optimizedBucket = backend.builder()
                .withOptimization(optimization)
                .buildProxy(1L, configuration)
            Bucket notOptimizedBucket = backend.builder()
                .buildProxy(1L, configuration)
            boolean consumed

        when: "first tryAcquire(1) happened"
            consumed = optimizedBucket.tryConsume(1)
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens() == 99
        and: "request propagated to backend"
            notOptimizedBucket.getAvailableTokens() == 99
        and: "metrics correctly counted"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 1 // getAvailableTokens increments this counter

        when: "next tryAcquire(1) happened after 9 millis"
            clock.addMillis(9) // 9
            consumed = optimizedBucket.tryConsume(1)
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens() == 98
        and: "request not propagated to backend because"
            notOptimizedBucket.getAvailableTokens() == 99
        and: "metrics correctly counted"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 3

        when: "next tryAcquire(1) happened after 1 millis"
            clock.addMillis(1) // 10
            consumed = optimizedBucket.tryConsume(1)
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens() == 98 // one token was refilled
        and: "request not propagated to backend"
            notOptimizedBucket.getAvailableTokens() == 100 // one token was refilled
        and: "metrics correctly counted"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 5

        when: "next tryAcquire(19) happened after 10 millis"
            clock.addMillis(10) // 20
            consumed = optimizedBucket.tryConsume(19)
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens() == 79
        and: "request propagated to backend because of overflow of delay threshold"
            notOptimizedBucket.getAvailableTokens() == 79 // one token was refilled
        and: "metrics correctly counted"
            listener.getMergeCount() == 0
            listener.getSkipCount() == 6

        when: "all tokens consumed from remote bucket"
            notOptimizedBucket.tryConsumeAsMuchAsPossible()
        then: "it is possible to consume from local bucket because sync timeout is not exceeded"
            optimizedBucket.tryConsume(1) == true
            optimizedBucket.getAvailableTokens() == 78
            optimizedBucket.tryConsumeAsMuchAsPossible(15) == 15
            optimizedBucket.getAvailableTokens() == 63

        when: "500 millis passed"
            clock.addMillis(500) // 500
        then: "request not propogated to backend"
            optimizedBucket.getAvailableTokens() == 100
            notOptimizedBucket.getAvailableTokens() == 50

        when: "500 millis passed"
            clock.addMillis(1) // 501
        then: "request propogated to backend"
            optimizedBucket.getAvailableTokens() == 34
            notOptimizedBucket.getAvailableTokens() == 34

        when: "too many optimized bucket overconsumed the bucket"
            List<Bucket> buckets = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                buckets.add(backend.builder().withOptimization(optimization).buildProxy(1L, configuration))
            }
            for (int i = 0; i < 10; i++) {
                buckets.get(i).getAvailableTokens() // just request needed to sync bucket with backend
                buckets.get(i).tryConsume(20);
            }
        then: "amount of token in the backend become negative"
            for (int i = 0; i < 10; i++) {
                buckets.get(i).tryConsume(1) == false;
            }
            notOptimizedBucket.getAvailableTokens() == -167
    }

    def "Should delay async consumption"() {
        setup:
            TimeMeterMock clock = new TimeMeterMock()
            GridBackendMock backend = new GridBackendMock(clock)
            DefaultOptimizationListener listener = new DefaultOptimizationListener();
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofMillis(1000)))
                .build()
            DelayParameters parameters = new DelayParameters(20, Duration.ofMillis(500))
            Optimization optimization = new DelayOptimization(parameters, listener, clock)
            AsyncBucketProxy optimizedBucket = backend.asAsync().builder()
                .withOptimization(optimization)
                .buildProxy(1L, configuration)
            Bucket notOptimizedBucket = backend.builder()
                .buildProxy(1L, configuration)
            boolean consumed

        when: "first tryAcquire(1) happened"
            consumed = optimizedBucket.tryConsume(1).get()
        then: "token was consumed"
            consumed == true
            optimizedBucket.getAvailableTokens().get() == 99
        and: "request propagated to backend"
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
        and: "request not propagated to backend because"
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
        and: "request not propagated to backend"
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
        and: "request propagated to backend because of overflow of delay threshold"
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
        then: "request not propogated to backend"
            optimizedBucket.getAvailableTokens().get() == 100
            notOptimizedBucket.getAvailableTokens() == 50

        when: "500 millis passed"
            clock.addMillis(1) // 501
        then: "request propogated to backend"
            optimizedBucket.getAvailableTokens().get() == 34
            notOptimizedBucket.getAvailableTokens() == 34

        when: "too many optimized bucket overconsumed the bucket"
            List<Bucket> buckets = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                buckets.add(backend.asAsync().builder().withOptimization(optimization).buildProxy(1L, configuration))
            }
            for (int i = 0; i < 10; i++) {
                buckets.get(i).getAvailableTokens() // just request needed to sync bucket with backend
                buckets.get(i).tryConsume(20);
            }
        then: "amount of token in the backend become negative"
            for (int i = 0; i < 10; i++) {
                buckets.get(i).tryConsume(1) == false;
            }
            notOptimizedBucket.getAvailableTokens() == -167
    }

}
