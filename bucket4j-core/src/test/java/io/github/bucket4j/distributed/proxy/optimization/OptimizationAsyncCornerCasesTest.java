package io.github.bucket4j.distributed.proxy.optimization;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.optimization.delay.DelayOptimization;
import io.github.bucket4j.distributed.proxy.optimization.manual.ManuallySyncingOptimization;
import io.github.bucket4j.distributed.proxy.optimization.predictive.PredictiveOptimization;
import io.github.bucket4j.distributed.proxy.optimization.skiponzero.SkipSyncOnZeroOptimization;
import io.github.bucket4j.mock.CompareAndSwapBasedProxyManagerMock;
import io.github.bucket4j.mock.ProxyManagerMock;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OptimizationAsyncCornerCases Specification")
class OptimizationAsyncCornerCasesTest {

    private record OptimizationCase(int testNumber, Optimization optimization) {

        @Override
        public String toString() {
            return "#" + testNumber;
        }
    }

    static Stream<OptimizationCase> optimizations() {
        return Stream.of(
            new OptimizationCase(1, Optimizations.batching()),
            new OptimizationCase(2, new DelayOptimization(DELAY_PARAMETERS, NopeOptimizationListener.INSTANCE, CLOCK)),
            new OptimizationCase(3, new PredictiveOptimization(PredictionParameters.createDefault(DELAY_PARAMETERS), DELAY_PARAMETERS, NopeOptimizationListener.INSTANCE, CLOCK)),
            new OptimizationCase(4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, CLOCK)),
            new OptimizationCase(5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, CLOCK))
        );
    }

    private static final DelayParameters DELAY_PARAMETERS = new DelayParameters(1, Duration.ofNanos(1));

    private static final TimeMeterMock CLOCK = new TimeMeterMock();

    // https://github.com/bucket4j/bucket4j/issues/398
    @ParameterizedTest
    @MethodSource("optimizations")
    void shouldCorrectlyHandleExceptionsWhenOptimizationIsUsedWithProxyManagerMock(OptimizationCase testCase) throws Exception {
        ProxyManagerMock proxyManagerMock = new ProxyManagerMock(CLOCK);
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();

        AsyncBucketProxy bucket = proxyManagerMock.asAsync().builder()
            .withOptimization(testCase.optimization())
            .build("66", configuration);

        assertThat(bucket.getAvailableTokens().get()).isEqualTo(10);
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume(1).get()).isTrue();
        }
        proxyManagerMock.removeProxy("66");

        assertThat(bucket.forceAddTokens(90).get()).isNull();
        assertThat(bucket.getAvailableTokens().get()).isEqualTo(100);

        proxyManagerMock.removeProxy("66");

        bucket.asVerbose().forceAddTokens(90).get();
        assertThat(bucket.asVerbose().getAvailableTokens().get().getValue()).isEqualTo(100);
    }

    // https://github.com/bucket4j/bucket4j/issues/398
    @ParameterizedTest
    @MethodSource("optimizations")
    void shouldCorrectlyHandleExceptionsWhenOptimizationIsUsedWithCompareAndSwapBasedProxyManagerMock(OptimizationCase testCase) throws Exception {
        CompareAndSwapBasedProxyManagerMock proxyManagerMock = new CompareAndSwapBasedProxyManagerMock(ClientSideConfig.getDefault().withClientClock(CLOCK));
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();

        AsyncBucketProxy bucket = proxyManagerMock.asAsync().builder()
            .withOptimization(testCase.optimization())
            .build("66", configuration);

        assertThat(bucket.getAvailableTokens().get()).isEqualTo(10);
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume(1).get()).isTrue();
        }
        proxyManagerMock.removeProxy("66");

        assertThat(bucket.forceAddTokens(90).get()).isNull();
        assertThat(bucket.getAvailableTokens().get()).isEqualTo(100);

        proxyManagerMock.removeProxy("66");

        bucket.asVerbose().forceAddTokens(90).get();
        assertThat(bucket.asVerbose().getAvailableTokens().get().getValue()).isEqualTo(100);
    }

}
