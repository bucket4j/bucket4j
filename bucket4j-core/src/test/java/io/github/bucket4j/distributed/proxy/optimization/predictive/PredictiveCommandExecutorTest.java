package io.github.bucket4j.distributed.proxy.optimization.predictive;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.optimization.DefaultOptimizationListener;
import io.github.bucket4j.distributed.proxy.optimization.DelayParameters;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;
import io.github.bucket4j.distributed.proxy.optimization.PredictionParameters;
import io.github.bucket4j.mock.ProxyManagerMock;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PredictiveCommandExecutorTest {

    private final TimeMeterMock clock = new TimeMeterMock();
    private final ProxyManagerMock<Long> proxyManager = new ProxyManagerMock<>(clock);
    private final DefaultOptimizationListener listener = new DefaultOptimizationListener();
    private final BucketConfiguration configuration = BucketConfiguration.builder()
        .addLimit(Bandwidth.simple(100, Duration.ofMillis(1000)))
        .build();
    private final DelayParameters delay = new DelayParameters(20, Duration.ofMillis(500));
    private final PredictionParameters prediction = PredictionParameters.createDefault(delay);
    private final Optimization optimization = new PredictiveOptimization(prediction, delay, listener, clock);
    private final Bucket optimizedBucket = proxyManager.builder()
        .withOptimization(optimization)
        .build(1L, configuration);
    private final Bucket notOptimizedBucket = proxyManager.builder()
        .build(1L, configuration);

    @Test
    void shouldDelaySyncConsumption() throws Exception {
        // when: first tryAcquire(1) happened
        boolean consumed = optimizedBucket.tryConsume(1);
        // then: token was consumed
        assertThat(consumed).isTrue();
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(99L);
        // and: request propagated to proxyManager
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(99L);
        // and: metrics correctly counted
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(0); // getAvailableTokens creates second sample

        // when: next tryAcquire(1) happened after 9 millis
        clock.addMillis(9); // 9
        consumed = optimizedBucket.tryConsume(1);
        // then: token was consumed
        assertThat(consumed).isTrue();
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(98L);
        // and: request not propagated to proxyManager because
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(99L);
        // and: metrics correctly counted
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(2);

        // when: next tryAcquire(1) happened after 1 millis
        clock.addMillis(1); // 10
        consumed = optimizedBucket.tryConsume(1);
        // then: token was consumed
        assertThat(consumed).isTrue();
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(98L); // one token was refilled
        // and: request not propagated to proxyManager
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(100L); // one token was refilled
        // and: metrics correctly counted
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(4);

        // when: next tryAcquire(19) happened after 10 millis
        clock.addMillis(10); // 20
        consumed = optimizedBucket.tryConsume(19);
        // then: token was consumed
        assertThat(consumed).isTrue();
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(79L);
        // and: request propagated to proxyManager because of overflow of delay threshold
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(79L); // one token was refilled
        // and: metrics correctly counted
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(5);

        // when: 50 tokens consumed from remote bucket
        notOptimizedBucket.tryConsume(75);
        // then: it is possible to consume from local bucket because sync timeout is not exceeded
        assertThat(optimizedBucket.tryConsume(1)).isTrue();
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(78L);
        assertThat(optimizedBucket.tryConsumeAsMuchAsPossible(15)).isEqualTo(15L);
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(63L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(4L);

        // when: 500 millis passed
        clock.addMillis(500); // 500
        // then: request not propogated to proxyManager
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(100L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(54L);

        // when: 1 millis passed
        clock.addMillis(1); // 501
        // then: request propogated to proxyManager
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(38L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(38L);

        // when: 250 millis passed
        clock.addMillis(250);
        // then: optimized bucket takes care about other nodes consumption rate
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(63L);
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(28L);

        // when: 125 millis passed
        clock.addMillis(125);
        // then: optimized bucket takes care about other nodes consumption rate
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(75L);
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(22L);

        // when:
        notOptimizedBucket.tryConsumeAsMuchAsPossible();
        optimizedBucket.tryConsume(19);

        // then: amount of token in the proxyManager become negative after sync
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(3L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(0L);

        // when:
        clock.addMillis(40);
        ((BucketProxy) optimizedBucket).getOptimizationController().syncImmediately();
        // then:
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(-15L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(-15L);

        // when:
        clock.addMillis(100);
        // then:
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(-5L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(-5L);

        // when:
        clock.addMillis(50);
        // then:
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(0L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(0L);

        // when:
        clock.addMillis(80);
        // then:
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(0L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(8L);

        // when:
        clock.addMillis(20);
        // then:
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(0L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(10L);

        // when:
        clock.addMillis(400);
        // then:
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(50L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(50L);
    }

    @Test
    void testSynchronizationByRequirement() throws Exception {
        // when: one token consumed without synchronization
        optimizedBucket.getAvailableTokens();
        clock.addMillis(1);
        optimizedBucket.getAvailableTokens();
        optimizedBucket.tryConsume(1);
        // then:
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(99L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(100L);

        // when: explicit synchronization request
        ((BucketProxy) optimizedBucket).getOptimizationController().syncImmediately();
        // then: synchronization performed
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(99L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(99L);
    }

    @Test
    void testSynchronizationByConditionalRequirement() throws Exception {
        // when: 10 tokens consumed without synchronization
        optimizedBucket.getAvailableTokens();
        clock.addTime(1);
        optimizedBucket.getAvailableTokens();
        optimizedBucket.tryConsume(10);
        // then:
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(90L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(100L);

        // when: synchronization requested with thresholds 20 tokens
        ((BucketProxy) optimizedBucket).getOptimizationController().syncByCondition(20, Duration.ZERO);
        // then: synchronization have not performed
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(90L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(100L);

        // when: synchronization requested with thresholds 10 tokens
        ((BucketProxy) optimizedBucket).getOptimizationController().syncByCondition(10, Duration.ZERO);
        // then: synchronization have not performed
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(90L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90L);

        // when: synchronization requested with thresholds 10 tokens
        ((BucketProxy) optimizedBucket).getOptimizationController().syncByCondition(10, Duration.ZERO);
        // then: synchronization have performed
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(90L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90L);

        // when: synchronization requested with thresholds 10 tokens
        ((BucketProxy) optimizedBucket).getOptimizationController().syncByCondition(10, Duration.ZERO);
        // then: synchronization have performed
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(90L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90L);

        // when: 9 millis passed 10 tokens consumed and synchronization requested with 20 millis limit
        clock.addMillis(9);
        optimizedBucket.tryConsume(10);
        ((BucketProxy) optimizedBucket).getOptimizationController().syncByCondition(10, Duration.ofMillis(20));
        // then: synchronization have not performed
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(80L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90L);

        // when: synchronization requested with limit 10 millis + 9 tokens
        ((BucketProxy) optimizedBucket).getOptimizationController().syncByCondition(9, Duration.ofMillis(10));
        // then: synchronization have not performed
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(80L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90L);

        // when: synchronization requested with limit 9 millis + 10 tokens
        ((BucketProxy) optimizedBucket).getOptimizationController().syncByCondition(10, Duration.ofMillis(9));
        // then: synchronization have performed
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(80L);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(80L);
    }

}
