
package io.github.bucket4j

import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.distributed.proxy.RemoteAsyncBucketBuilder
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronizations
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture

class ImplicitConfigurationReplacementSpecification extends Specification {

    static int key = 42

    @Unroll
    def "#bucketType should replace configuration implicitly when version was not provided previously"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose: [true, false]) {
                for (boolean batching: [true, false]) {
                    // System.err.println("sync: $sync verbose: $verbose $batching")
                    BucketConfiguration oldConfiguration = BucketConfiguration.builder()
                        .addLimit({it.capacity(60).refillGreedy(60, Duration.ofNanos(1000))})
                        .build()
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit({it.capacity(3).refillGreedy(3, Duration.ofNanos(5))})
                        .build()

                    TimeMeterMock clock = new TimeMeterMock(0)
                    ProxyManager<Integer> proxyManager = bucketType.createProxyManager(clock)

                    if (!proxyManager.isAsyncModeSupported() && !sync) {
                        continue
                    }

                    if (sync) {
                        RemoteBucketBuilder<Integer> builder = proxyManager.builder()
                        if (batching) {
                            builder.withSynchronization(BucketSynchronizations.batching())
                        }
                        Bucket bucket1 = builder.build(key, () -> oldConfiguration)
                        assert bucket1.getAvailableTokens() == 60

                        builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS)
                        Bucket bucket2 = builder.build(key, () -> newConfiguration)

                        if (!verbose) {
                            assert bucket2.getAvailableTokens() == 3
                        } else {
                            bucket2.asVerbose().getAvailableTokens().value == 3
                        }
                    } else {
                        RemoteAsyncBucketBuilder<Integer> builder = proxyManager.asAsync().builder()
                        if (batching) {
                            builder.withSynchronization(BucketSynchronizations.batching())
                        }

                        AsyncBucketProxy bucket1 = builder.build(key, {CompletableFuture.completedFuture(oldConfiguration)})
                        assert bucket1.getAvailableTokens().get() == 60

                        builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS)
                        AsyncBucketProxy bucket2 = builder.build(key, {CompletableFuture.completedFuture(newConfiguration)})

                        if (!verbose) {
                            assert bucket2.getAvailableTokens().get() == 3
                        } else {
                            bucket2.asVerbose().getAvailableTokens().get().value == 3
                        }
                    }
                }
            }
        }
        where:
            bucketType << [BucketType.GRID, BucketType.COMPARE_AND_SWAP, BucketType.LOCK_BASED, BucketType.SELECT_FOR_UPDATE]
    }

    @Unroll
    def "#bucketType should replace configuration implicitly when previous version less than current"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose: [true, false]) {
                for (boolean batching: [true, false]) {
                    // System.err.println("sync: $sync verbose: $verbose $batching")
                    BucketConfiguration oldConfiguration = BucketConfiguration.builder()
                        .addLimit({it.capacity(60).refillGreedy(60, Duration.ofNanos(1000))})
                        .build()
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit({it.capacity(3).refillGreedy(3, Duration.ofNanos(5))})
                        .build()

                    TimeMeterMock clock = new TimeMeterMock(0)
                    ProxyManager<Integer> proxyManager = bucketType.createProxyManager(clock)

                    if (!proxyManager.isAsyncModeSupported() && !sync) {
                        continue
                    }

                    if (sync) {
                        RemoteBucketBuilder<Integer> builder = proxyManager.builder()
                        if (batching) {
                            builder.withSynchronization(BucketSynchronizations.batching())
                        }
                        Bucket bucket1 = builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS).build(key, {oldConfiguration})
                        assert bucket1.getAvailableTokens() == 60

                        builder.withImplicitConfigurationReplacement(2L, TokensInheritanceStrategy.AS_IS)
                        Bucket bucket2 = builder.build(key, {newConfiguration})

                        if (!verbose) {
                            assert bucket2.getAvailableTokens() == 3
                        } else {
                            bucket2.asVerbose().getAvailableTokens().value == 3
                        }
                    } else {
                        RemoteAsyncBucketBuilder<Integer> builder = proxyManager.asAsync().builder()
                        if (batching) {
                            builder.withSynchronization(BucketSynchronizations.batching())
                        }

                        AsyncBucketProxy bucket1 = builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS)
                                .build(key, {CompletableFuture.completedFuture(oldConfiguration)})
                        assert bucket1.getAvailableTokens().get() == 60

                        builder.withImplicitConfigurationReplacement(2L, TokensInheritanceStrategy.AS_IS)
                        AsyncBucketProxy bucket2 = builder.build(key, {CompletableFuture.completedFuture(newConfiguration)})

                        if (!verbose) {
                            assert bucket2.getAvailableTokens().get() == 3
                        } else {
                            bucket2.asVerbose().getAvailableTokens().get().value == 3
                        }
                    }
                }
            }
        }
        where:
            bucketType << [BucketType.GRID, BucketType.COMPARE_AND_SWAP, BucketType.LOCK_BASED, BucketType.SELECT_FOR_UPDATE]
    }

    @Unroll
    def "#bucketType should not replace configuration implicitly when previous version equals with current"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose: [true, false]) {
                for (boolean batching: [true, false]) {
                    // System.err.println("sync: $sync verbose: $verbose $batching")
                    BucketConfiguration oldConfiguration = BucketConfiguration.builder()
                        .addLimit({it.capacity(60).refillGreedy(60, Duration.ofNanos(1000))})
                        .build()
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit({it.capacity(3).refillGreedy(3, Duration.ofNanos(5))})
                        .build()

                    TimeMeterMock clock = new TimeMeterMock(0)
                    ProxyManager<Integer> proxyManager = bucketType.createProxyManager(clock)

                    if (!proxyManager.isAsyncModeSupported() && !sync) {
                        continue
                    }

                    if (sync) {
                        RemoteBucketBuilder<Integer> builder = proxyManager.builder()
                        if (batching) {
                            builder.withSynchronization(BucketSynchronizations.batching())
                        }
                        Bucket bucket1 = builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS).build(key, {oldConfiguration})
                        assert bucket1.getAvailableTokens() == 60

                        builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS)
                        Bucket bucket2 = builder.build(key, {newConfiguration})

                        if (!verbose) {
                            assert bucket2.getAvailableTokens() == 60
                        } else {
                            bucket2.asVerbose().getAvailableTokens().value == 60
                        }
                    } else {
                        RemoteAsyncBucketBuilder<Integer> builder = proxyManager.asAsync().builder()
                        if (batching) {
                            builder.withSynchronization(BucketSynchronizations.batching())
                        }

                        AsyncBucketProxy bucket1 = builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS)
                                .build(key, {CompletableFuture.completedFuture(oldConfiguration)})
                        assert bucket1.getAvailableTokens().get() == 60

                        builder.withImplicitConfigurationReplacement(1L, TokensInheritanceStrategy.AS_IS)
                        AsyncBucketProxy bucket2 = builder.build(key, {CompletableFuture.completedFuture(newConfiguration)})

                        if (!verbose) {
                            assert bucket2.getAvailableTokens().get() == 60
                        } else {
                            bucket2.asVerbose().getAvailableTokens().get().value == 60
                        }
                    }
                }
            }
        }
        where:
            bucketType << [BucketType.GRID, BucketType.COMPARE_AND_SWAP, BucketType.LOCK_BASED, BucketType.SELECT_FOR_UPDATE]
    }

}
