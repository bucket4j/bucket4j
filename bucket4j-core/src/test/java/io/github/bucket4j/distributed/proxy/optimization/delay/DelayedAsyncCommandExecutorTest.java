package io.github.bucket4j.distributed.proxy.optimization.delay;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.optimization.DefaultOptimizationListener;
import io.github.bucket4j.distributed.proxy.optimization.DelayParameters;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;
import io.github.bucket4j.mock.ProxyManagerMock;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DelayedAsyncCommandExecutor Specification")
class DelayedAsyncCommandExecutorTest {

    private final TimeMeterMock clock = new TimeMeterMock();
    private final ProxyManagerMock proxyManager = new ProxyManagerMock(clock);
    private final DefaultOptimizationListener listener = new DefaultOptimizationListener();
    private final BucketConfiguration configuration = BucketConfiguration.builder()
        .addLimit(Bandwidth.simple(100, Duration.ofMillis(1000)))
        .build();
    private final DelayParameters parameters = new DelayParameters(20, Duration.ofMillis(500));
    private final Optimization optimization = new DelayOptimization(parameters, listener, clock);
    private final AsyncBucketProxy optimizedBucket = proxyManager.asAsync().builder()
        .withOptimization(optimization)
        .build(1L, configuration);
    private final Bucket notOptimizedBucket = proxyManager.builder()
        .build(1L, configuration);

    @Test
    void shouldDelayAsyncConsumption() throws Exception {
        // first tryAcquire(1) happened
        boolean consumed = optimizedBucket.tryConsume(1).get();
        // token was consumed
        assertThat(consumed).isTrue();
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(99);
        // request propagated to proxyManager
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(99);
        // metrics correctly counted
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(1); // getAvailableTokens increments this counter

        // next tryAcquire(1) happened after 9 millis
        clock.addMillis(9); // 9
        consumed = optimizedBucket.tryConsume(1).get();
        // token was consumed
        assertThat(consumed).isTrue();
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(98);
        // request not propagated to proxyManager because
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(99);
        // metrics correctly counted
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(3);

        // next tryAcquire(1) happened after 1 millis
        clock.addMillis(1); // 10
        consumed = optimizedBucket.tryConsume(1).get();
        // token was consumed
        assertThat(consumed).isTrue();
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(98); // one token was refilled
        // request not propagated to proxyManager
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(100); // one token was refilled
        // metrics correctly counted
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(5);

        // next tryAcquire(19) happened after 10 millis
        clock.addMillis(10); // 20
        consumed = optimizedBucket.tryConsume(19).get();
        // token was consumed
        assertThat(consumed).isTrue();
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(79);
        // request propagated to proxyManager because of overflow of delay threshold
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(79); // one token was refilled
        // metrics correctly counted
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(6);

        // all tokens consumed from remote bucket
        notOptimizedBucket.tryConsumeAsMuchAsPossible();
        // it is possible to consume from local bucket because sync timeout is not exceeded
        assertThat(optimizedBucket.tryConsume(1).get()).isTrue();
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(78);
        assertThat(optimizedBucket.tryConsumeAsMuchAsPossible(15).get()).isEqualTo(15);
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(63);

        // 500 millis passed
        clock.addMillis(500); // 500
        // request not propogated to proxyManager
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(100);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(50);

        // 1 millis passed
        clock.addMillis(1); // 501
        // request propogated to proxyManager
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(34);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(34);

        // too many optimized bucket overconsumed the bucket
        List<AsyncBucketProxy> buckets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            buckets.add(proxyManager.asAsync().builder().withOptimization(optimization).build(1L, configuration));
        }
        for (int i = 0; i < 10; i++) {
            buckets.get(i).getAvailableTokens(); // just request needed to sync bucket with proxyManager
            buckets.get(i).tryConsume(20);
        }
        // amount of token in the proxyManager become negative
        for (int i = 0; i < 10; i++) {
            buckets.get(i).tryConsume(1);
        }
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(-167);
    }

    @Test
    void testSynchronizationByRequirement() throws Exception {
        // one token consumed without synchronization
        optimizedBucket.getAvailableTokens().get();
        optimizedBucket.tryConsume(1).get();

        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(99);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(100);

        // explicit synchronization request
        optimizedBucket.getOptimizationController().syncImmediately().get();

        // synchronization performed
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(99);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(99);
    }

    @Test
    void testSynchronizationByConditionalRequirement() throws Exception {
        // 10 tokens consumed without synchronization
        optimizedBucket.getAvailableTokens().get();
        optimizedBucket.tryConsume(10).get();

        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(90);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(100);

        // synchronization requested with thresholds 20 tokens
        optimizedBucket.getOptimizationController().syncByCondition(20, Duration.ZERO).get();
        // synchronization have not performed
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(90);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(100);

        // synchronization requested with thresholds 10 tokens
        optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ZERO).get();
        // synchronization have not performed
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(90);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90);

        // synchronization requested with thresholds 10 tokens
        optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ZERO).get();
        // synchronization have performed
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(90);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90);

        // synchronization requested with thresholds 10 tokens
        optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ZERO).get();
        // synchronization have performed
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(90);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90);

        // 9 millis passed 10 tokens consumed and synchronization requested with 20 millis limit
        clock.addMillis(9);
        optimizedBucket.tryConsume(10).get();
        optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ofMillis(20)).get();
        // synchronization have not performed
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(80);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90);

        // synchronization requested with limit 10 millis + 9 tokens
        optimizedBucket.getOptimizationController().syncByCondition(9, Duration.ofMillis(10)).get();
        // synchronization have not performed
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(80);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(90);

        // synchronization requested with limit 9 millis + 10 tokens
        optimizedBucket.getOptimizationController().syncByCondition(10, Duration.ofMillis(9)).get();
        // synchronization have performed
        assertThat(optimizedBucket.getAvailableTokens().get()).isEqualTo(80);
        assertThat(notOptimizedBucket.getAvailableTokens()).isEqualTo(80);
    }

}
