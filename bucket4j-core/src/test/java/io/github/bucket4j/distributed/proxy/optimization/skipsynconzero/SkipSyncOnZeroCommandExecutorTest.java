package io.github.bucket4j.distributed.proxy.optimization.skipsynconzero;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.optimization.DefaultOptimizationListener;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;
import io.github.bucket4j.distributed.proxy.optimization.skiponzero.SkipSyncOnZeroOptimization;
import io.github.bucket4j.mock.ProxyManagerMock;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SkipSyncOnZeroCommandExecutorTest {

    private final TimeMeterMock clock = new TimeMeterMock();
    private final ProxyManagerMock<Long> proxyManager = new ProxyManagerMock<>(clock);
    private final DefaultOptimizationListener listener = new DefaultOptimizationListener();
    private final BucketConfiguration configuration = BucketConfiguration.builder()
        .addLimit(Bandwidth.simple(100, Duration.ofMillis(1000)))
        .build();
    private final Optimization optimization = new SkipSyncOnZeroOptimization(listener, clock);
    private final Bucket optimizedBucket = proxyManager.builder()
        .withOptimization(optimization)
        .build(1L, configuration);

    @Test
    void shouldSkipSynchronizationWithStorageWhenBucketIsEmpty() throws Exception {
        // when: bucket becomes empty
        optimizedBucket.tryConsumeAsMuchAsPossible();
        // and: trying to consume again
        optimizedBucket.tryConsume(1);
        // then: request should not be propogated to server
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(1);
    }

    @Test
    void shouldCorrectlyCalculateTheTimeOfNextSyncWithStorageWithStorage() throws Exception {
        // when: bucket becomes empty
        optimizedBucket.tryConsumeAsMuchAsPossible();
        // and: trying to consume again
        optimizedBucket.tryConsume(1);
        // then: request should not be propogated to server
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(1);

        // when: past enough time to generate single token
        clock.addMillis(10);
        // and: trying to consume again
        boolean consumed = optimizedBucket.tryConsume(1);
        // then: consumption request should be propogated to storage
        assertThat(consumed).isTrue();
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(1);
    }

    @Test
    void specialCommandsShouldLeadToImmediatelySynchronizationWithServer() throws Exception {
        // when: bucket becomes empty
        optimizedBucket.tryConsumeAsMuchAsPossible();
        // and: reset all limits
        optimizedBucket.reset();
        // then: request should not be propogated to server
        assertThat(listener.getMergeCount()).isEqualTo(0);
        assertThat(listener.getSkipCount()).isEqualTo(0);
        assertThat(optimizedBucket.getAvailableTokens()).isEqualTo(100L);
    }

}
