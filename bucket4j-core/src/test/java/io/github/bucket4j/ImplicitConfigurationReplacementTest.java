package io.github.bucket4j;

import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteAsyncBucketBuilder;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.distributed.proxy.optimization.Optimizations;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImplicitConfigurationReplacement Specification")
class ImplicitConfigurationReplacementTest {

    private record BucketTypeCase(BucketType bucketType) {

        @Override
        public String toString() {
            return "bucketType=" + bucketType;
        }
    }

    static Stream<BucketTypeCase> bucketTypes() {
        return Stream.of(BucketType.GRID, BucketType.COMPARE_AND_SWAP, BucketType.LOCK_BASED, BucketType.SELECT_FOR_UPDATE)
            .map(BucketTypeCase::new);
    }

    static int key = 42;

    @Nested
    class SyncBucket {

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ImplicitConfigurationReplacementTest#bucketTypes")
        void shouldReplaceConfigurationImplicitlyWhenVersionWasNotProvidedPreviously(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                for (boolean batching : new boolean[]{true, false}) {
                    BucketConfiguration oldConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build();
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)))
                        .build();

                    TimeMeterMock clock = new TimeMeterMock(0);
                    ProxyManager<Integer> proxyManager = testCase.bucketType().createProxyManager(clock);

                    RemoteBucketBuilder<Integer> builder = proxyManager.builder();
                    if (batching) {
                        builder.withOptimization(Optimizations.batching());
                    }
                    Bucket bucket1 = builder.build(key, oldConfiguration);
                    assertThat(bucket1.getAvailableTokens()).isEqualTo(60);

                    builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS);
                    Bucket bucket2 = builder.build(key, newConfiguration);

                    if (!verbose) {
                        assertThat(bucket2.getAvailableTokens()).isEqualTo(3);
                    } else {
                        assertThat(bucket2.asVerbose().getAvailableTokens().getValue()).isEqualTo(3);
                    }
                }
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ImplicitConfigurationReplacementTest#bucketTypes")
        void shouldReplaceConfigurationImplicitlyWhenPreviousVersionLessThanCurrent(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                for (boolean batching : new boolean[]{true, false}) {
                    BucketConfiguration oldConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build();
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)))
                        .build();

                    TimeMeterMock clock = new TimeMeterMock(0);
                    ProxyManager<Integer> proxyManager = testCase.bucketType().createProxyManager(clock);

                    RemoteBucketBuilder<Integer> builder = proxyManager.builder();
                    if (batching) {
                        builder.withOptimization(Optimizations.batching());
                    }
                    Bucket bucket1 = builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS).build(key, oldConfiguration);
                    assertThat(bucket1.getAvailableTokens()).isEqualTo(60);

                    builder.withImplicitConfigurationReplacement(2L, TokensInheritanceStrategy.AS_IS);
                    Bucket bucket2 = builder.build(key, newConfiguration);

                    if (!verbose) {
                        assertThat(bucket2.getAvailableTokens()).isEqualTo(3);
                    } else {
                        assertThat(bucket2.asVerbose().getAvailableTokens().getValue()).isEqualTo(3);
                    }
                }
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ImplicitConfigurationReplacementTest#bucketTypes")
        void shouldNotReplaceConfigurationImplicitlyWhenPreviousVersionEqualsWithCurrent(BucketTypeCase testCase) {
            for (boolean verbose : new boolean[]{true, false}) {
                for (boolean batching : new boolean[]{true, false}) {
                    BucketConfiguration oldConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build();
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)))
                        .build();

                    TimeMeterMock clock = new TimeMeterMock(0);
                    ProxyManager<Integer> proxyManager = testCase.bucketType().createProxyManager(clock);

                    RemoteBucketBuilder<Integer> builder = proxyManager.builder();
                    if (batching) {
                        builder.withOptimization(Optimizations.batching());
                    }
                    Bucket bucket1 = builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS).build(key, oldConfiguration);
                    assertThat(bucket1.getAvailableTokens()).isEqualTo(60);

                    builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS);
                    Bucket bucket2 = builder.build(key, newConfiguration);

                    if (!verbose) {
                        assertThat(bucket2.getAvailableTokens()).isEqualTo(60);
                    } else {
                        assertThat(bucket2.asVerbose().getAvailableTokens().getValue()).isEqualTo(60);
                    }
                }
            }
        }

    }

    @Nested
    class AsyncBucket {

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ImplicitConfigurationReplacementTest#bucketTypes")
        void shouldReplaceConfigurationImplicitlyWhenVersionWasNotProvidedPreviously(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                for (boolean batching : new boolean[]{true, false}) {
                    BucketConfiguration oldConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build();
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)))
                        .build();

                    TimeMeterMock clock = new TimeMeterMock(0);
                    ProxyManager<Integer> proxyManager = testCase.bucketType().createProxyManager(clock);

                    if (!proxyManager.isAsyncModeSupported()) {
                        continue;
                    }

                    RemoteAsyncBucketBuilder<Integer> builder = proxyManager.asAsync().builder();
                    if (batching) {
                        builder.withOptimization(Optimizations.batching());
                    }

                    AsyncBucketProxy bucket1 = builder.build(key, oldConfiguration);
                    assertThat(bucket1.getAvailableTokens().get()).isEqualTo(60);

                    builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS);
                    AsyncBucketProxy bucket2 = builder.build(key, newConfiguration);

                    if (!verbose) {
                        assertThat(bucket2.getAvailableTokens().get()).isEqualTo(3);
                    } else {
                        assertThat(bucket2.asVerbose().getAvailableTokens().get().getValue()).isEqualTo(3);
                    }
                }
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ImplicitConfigurationReplacementTest#bucketTypes")
        void shouldReplaceConfigurationImplicitlyWhenPreviousVersionLessThanCurrent(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                for (boolean batching : new boolean[]{true, false}) {
                    BucketConfiguration oldConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build();
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)))
                        .build();

                    TimeMeterMock clock = new TimeMeterMock(0);
                    ProxyManager<Integer> proxyManager = testCase.bucketType().createProxyManager(clock);

                    if (!proxyManager.isAsyncModeSupported()) {
                        continue;
                    }

                    RemoteAsyncBucketBuilder<Integer> builder = proxyManager.asAsync().builder();
                    if (batching) {
                        builder.withOptimization(Optimizations.batching());
                    }

                    AsyncBucketProxy bucket1 = builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS).build(key, oldConfiguration);
                    assertThat(bucket1.getAvailableTokens().get()).isEqualTo(60);

                    builder.withImplicitConfigurationReplacement(2L, TokensInheritanceStrategy.AS_IS);
                    AsyncBucketProxy bucket2 = builder.build(key, newConfiguration);

                    if (!verbose) {
                        assertThat(bucket2.getAvailableTokens().get()).isEqualTo(3);
                    } else {
                        assertThat(bucket2.asVerbose().getAvailableTokens().get().getValue()).isEqualTo(3);
                    }
                }
            }
        }

        @ParameterizedTest
        @MethodSource("io.github.bucket4j.ImplicitConfigurationReplacementTest#bucketTypes")
        void shouldNotReplaceConfigurationImplicitlyWhenPreviousVersionEqualsWithCurrent(BucketTypeCase testCase) throws Exception {
            for (boolean verbose : new boolean[]{true, false}) {
                for (boolean batching : new boolean[]{true, false}) {
                    BucketConfiguration oldConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build();
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)))
                        .build();

                    TimeMeterMock clock = new TimeMeterMock(0);
                    ProxyManager<Integer> proxyManager = testCase.bucketType().createProxyManager(clock);

                    if (!proxyManager.isAsyncModeSupported()) {
                        continue;
                    }

                    RemoteAsyncBucketBuilder<Integer> builder = proxyManager.asAsync().builder();
                    if (batching) {
                        builder.withOptimization(Optimizations.batching());
                    }

                    AsyncBucketProxy bucket1 = builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS).build(key, oldConfiguration);
                    assertThat(bucket1.getAvailableTokens().get()).isEqualTo(60);

                    builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS);
                    AsyncBucketProxy bucket2 = builder.build(key, newConfiguration);

                    if (!verbose) {
                        assertThat(bucket2.getAvailableTokens().get()).isEqualTo(60);
                    } else {
                        assertThat(bucket2.asVerbose().getAvailableTokens().get().getValue()).isEqualTo(60);
                    }
                }
            }
        }

    }

}
