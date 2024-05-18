
package io.github.bucket4j

import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class ConfigurationReplacementSpecification extends Specification {

    @Unroll
    def "#bucketType should forget about previously consumed tokens when replace configuration in reset mode"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose: [true, false]) {
                System.err.println("sync: $sync verbose: $verbose")
                TimeMeterMock clock = new TimeMeterMock(0)
                BucketConfiguration configuration = BucketConfiguration.builder()
                        .addLimit({it.capacity(3).refillGreedy(3, Duration.ofNanos(5)).initialTokens(0)})
                        .build()
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit({it.capacity(60).refillGreedy(60, Duration.ofNanos(1000))})
                        .build()
                if (sync) {
                    Bucket bucket = bucketType.createBucket(configuration, clock)
                    clock.addTime(3) // 1.8
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET)
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET)
                    }
                    assert bucket.getAvailableTokens() == 60
                } else {
                    AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                    clock.addTime(3) // 1.8
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET).get()
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET).get()
                    }
                    assert bucket.getAvailableTokens().get() == 60
                }
            }
        }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should forget about previously consumed tokens when replace configuration for bandwidth which not matched by id"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                for (boolean verbose: [true, false]) {
                    // System.err.println("sync: $sync verbose: $verbose")
                    TimeMeterMock clock = new TimeMeterMock(0)
                    def configuration = BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(3).refillGreedy(3, Duration.ofNanos(5)).initialTokens(0).id("x"))
                            .build()
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(60).refillGreedy(60, Duration.ofNanos(1000)).id("y"))
                            .build()
                    if (sync) {
                        Bucket bucket = bucketType.createBucket(configuration, clock)
                        clock.addTime(3) // 1.8
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS)
                        }
                        assert bucket.getAvailableTokens() == 60
                    } else {
                        AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                        clock.addTime(3) // 1.8
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET).get()
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.RESET).get()
                        }
                        assert bucket.getAvailableTokens().get() == 60
                    }
                }
            }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should match bandwidth by id during configuration replacement"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                for (boolean verbose: [true, false]) {
                    // System.err.println("sync: $sync verbose: $verbose")
                    TimeMeterMock clock = new TimeMeterMock(0)
                    def configuration = BucketConfiguration.builder()
                        .addLimit(limit -> limit.capacity(3).refillGreedy(3, Duration.ofNanos(5)).initialTokens(0).id("x"))
                        .addLimit(limit -> limit.capacity(1000).refillGreedy(1000, Duration.ofNanos(1000)).initialTokens(1).id("z"))
                        .build()
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(limit -> limit.capacity(60).refillGreedy(60, Duration.ofNanos(1000)).id("y"))
                        .addLimit(limit -> limit.capacity(1000).refillGreedy(1000, Duration.ofNanos(1000)).id("z"))
                        .build()
                    if (sync) {
                        Bucket bucket = bucketType.createBucket(configuration, clock)
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS)
                        }
                        assert bucket.getAvailableTokens() == 1
                        def snapshot = bucket.asVerbose().getAvailableTokens().getState()
                        assert snapshot.getCurrentSize(0) == 60
                        assert snapshot.getCurrentSize(1) == 1
                    } else {
                        AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get()
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get()
                        }
                        assert bucket.getAvailableTokens().get() == 1
                    }
                }
            }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType test replace configuration as_is from gready refill to gready refill"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                for (boolean verbose: [true, false]) {
                    System.err.println("sync: $sync verbose: $verbose")
                    TimeMeterMock clock = new TimeMeterMock(0)
                    BucketConfiguration configuration = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                            .build()
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                            .build()
                    if (sync) {
                        Bucket bucket = bucketType.createBucket(configuration, clock)
                        assert bucket.getAvailableTokens() == 0

                        clock.addTime(3) // 1.8
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS)
                        }
                        assert bucket.getAvailableTokens() == 1

                        clock.addTime(4)
                        assert bucket.getAvailableTokens() == 2
                    } else {
                        AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                        assert bucket.getAvailableTokens().get() == 0

                        clock.addTime(3) // 1.8
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get()
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get()
                        }
                        assert bucket.getAvailableTokens().get() == 1

                        clock.addTime(4)
                        assert bucket.getAvailableTokens().get() == 2
                    }
                }
            }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType test replace configuration as_is from gready refill to intervally refill"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                for (boolean verbose: [true, false]) {
                    // System.err.println("sync: $sync verbose: $verbose")
                    TimeMeterMock clock = new TimeMeterMock(0)
                    BucketConfiguration configuration = BucketConfiguration.builder()
                            .addLimit({it.capacity(3).refillGreedy(3, Duration.ofNanos(5)).initialTokens(0)})
                            .build()
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                            .addLimit({it.capacity(60).refillIntervally(60, Duration.ofNanos(1000))})
                            .build()
                    if (sync) {
                        Bucket bucket = bucketType.createBucket(configuration, clock)
                        bucket.getAvailableTokens()
                        clock.addTime(3) // 1.8
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS)
                        }
                        assert bucket.getAvailableTokens() == 1

                        clock.addTime(999)
                        assert bucket.getAvailableTokens() == 1 // 0.8 tokens from previous bucket should not be copied after config replacement

                        clock.addTime(1)
                        assert bucket.getAvailableTokens() == 60
                    } else {
                        def bucket = bucketType.createAsyncBucket(configuration, clock)
                        bucket.getAvailableTokens()
                        clock.addTime(3) // 1.8
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get()
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get()
                        }
                        assert bucket.getAvailableTokens().get() == 1

                        clock.addTime(999)
                        assert bucket.getAvailableTokens() .get()== 1 // 0.8 tokens from previous bucket should not be copied after config replacement

                        clock.addTime(1)
                        assert bucket.getAvailableTokens().get() == 60
                    }
                }
            }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should decrease available tokens when reducing capacity and copying tokens as_is"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            TimeMeterMock clock = new TimeMeterMock(0)
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                    .build()
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic (200, Refill.greedy(100, Duration.ofNanos(100)) ))
                    .build()
            if (sync) {
                Bucket bucket = bucketType.createBucket(configuration, clock)
                bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS)
                assert bucket.getAvailableTokens() == 200
            } else {
                AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.AS_IS).get()
                assert bucket.getAvailableTokens().get() == 200
            }
        }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType test replace configuration additive from gready refill to gready refill  when capacity increased"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose: [true, false]) {
                // System.err.println("sync: $sync verbose: $verbose")
                TimeMeterMock clock = new TimeMeterMock(0)
                BucketConfiguration configuration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                        .build()
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build()
                if (sync) {
                    Bucket bucket = bucketType.createBucket(configuration, clock)
                    bucket.getAvailableTokens()
                    clock.addTime(3) // 1.8
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE)
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE)
                    }
                    assert bucket.getAvailableTokens() == 58

                    clock.addTime(4)
                    assert bucket.getAvailableTokens() == 59
                } else {
                    AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                    bucket.getAvailableTokens().get()
                    clock.addTime(3) // 1.8
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get()
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get()
                    }
                    assert bucket.getAvailableTokens().get() == 58

                    clock.addTime(4)
                    assert bucket.getAvailableTokens().get() == 59
                }
            }
        }
        where:
        bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType test replace configuration additive from gready refill to intervally refill when capacity increased"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose: [true, false]) {
                // System.err.println("sync: $sync verbose: $verbose")
                TimeMeterMock clock = new TimeMeterMock(0)
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build()
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(60).refillIntervally(60, Duration.ofNanos(1000)))
                    .build()
                if (sync) {
                    Bucket bucket = bucketType.createBucket(configuration, clock)
                    bucket.getAvailableTokens();
                    clock.addTime(3) // 1.8
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE)
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE)
                    }
                    assert bucket.getAvailableTokens() == 58

                    clock.addTime(999)
                    assert bucket.getAvailableTokens() == 58 // 0.8 tokens from previous bucket should not be copied after config replacement

                    clock.addTime(1)
                    assert bucket.getAvailableTokens() == 60
                } else {
                    AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                    bucket.getAvailableTokens().get();
                    clock.addTime(3) // 1.8
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get()
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get()
                    }
                    assert bucket.getAvailableTokens().get() == 58

                    clock.addTime(999)
                    assert bucket.getAvailableTokens().get() == 58 // 0.8 tokens from previous bucket should not be copied after config replacement

                    clock.addTime(1)
                    assert bucket.getAvailableTokens().get() == 60
                }
            }
        }
        where:
        bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should decrease available tokens when reducing capacity and copying tokens additive"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            TimeMeterMock clock = new TimeMeterMock(0)
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                    .build()
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic (200, Refill.greedy(100, Duration.ofNanos(100)) ))
                    .build()
            if (sync) {
                Bucket bucket = bucketType.createBucket(configuration, clock)
                bucket.getAvailableTokens()
                bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE)
                assert bucket.getAvailableTokens() == 200
            } else {
                AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get()
                assert bucket.getAvailableTokens().get() == 200
            }
        }
        where:
        bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should increase available tokens when reducing capacity and copying tokens additive"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            TimeMeterMock clock = new TimeMeterMock(0)

            def configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))).withInitialTokens(200)).build()
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic (900, Refill.greedy(100, Duration.ofNanos(100)) ))
                    .build()
            if (sync) {
                Bucket bucket = bucketType.createBucket(configuration, clock)
                bucket.getAvailableTokens()
                bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE)
                assert bucket.getAvailableTokens() == 600
            } else {
                AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                bucket.getAvailableTokens().get()
                bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.ADDITIVE).get()
                assert bucket.getAvailableTokens().get() == 600
            }
        }
        where:
        bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should decrease available tokens when reducing capacity and copying tokens proportionally"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                TimeMeterMock clock = new TimeMeterMock(0)
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                    .build()
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic (200, Refill.greedy(100, Duration.ofNanos(100))))
                    .build()
                if (sync) {
                    Bucket bucket = bucketType.createBucket(configuration, clock)
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY)
                    assert bucket.getAvailableTokens() == 200
                } else {
                    AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                    bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get()
                    assert bucket.getAvailableTokens().get() == 200
                }
            }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType test replace configuration proportionally from gready refill to gready refill"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose: [true, false]) {
                // System.err.println("sync: $sync verbose: $verbose")
                TimeMeterMock clock = new TimeMeterMock(0)
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build()
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                    .build()
                if (sync) {
                    Bucket bucket = bucketType.createBucket(configuration, clock)
                    bucket.getAvailableTokens()
                    clock.addTime(3) // 1.8
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY)
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY)
                    }
                    assert bucket.getAvailableTokens() == 36

                    clock.addTime(4)
                    assert bucket.getAvailableTokens() == 36

//                def snapshot = bucket.createSnapshot()
//                System.err.println("bucketType: $bucketType, sync: $sync verbose: $verbose availableTokens: ${snapshot.getCurrentSize(0)}  roundingError: ${snapshot.getRoundingError(0)}")

                    clock.addTime(13)
                    assert bucket.getAvailableTokens() == 37
                } else {
                    AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                    bucket.getAvailableTokens()
                    clock.addTime(3) // 1.8
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get()
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get()
                    }
                    assert bucket.getAvailableTokens().get() == 36

                    clock.addTime(4)
                    assert bucket.getAvailableTokens().get() == 36

//                def snapshot = bucket.createSnapshot()
//                System.err.println("bucketType: $bucketType, sync: $sync verbose: $verbose availableTokens: ${snapshot.getCurrentSize(0)}  roundingError: ${snapshot.getRoundingError(0)}")

                    clock.addTime(13)
                    assert bucket.getAvailableTokens().get() == 37
                }
            }
        }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType test replace configuration proportionally when capacity overflown"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose: [true, false]) {
                // System.err.println("sync: $sync verbose: $verbose")
                TimeMeterMock clock = new TimeMeterMock(0)
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0))
                    .build()

                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build()
                if (sync) {
                    Bucket bucket = bucketType.createBucket(configuration, clock)
                    bucket.forceAddTokens(10000000)
                    assert bucket.getAvailableTokens() == 10000000
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY)
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY)
                    }
                    assert bucket.getAvailableTokens() == 60 // because should be just reduced to maximum
                } else {
                    AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock)
                    bucket.forceAddTokens(10000000)
                    assert bucket.getAvailableTokens().get() == 10000000
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get()
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get()
                    }
                    assert bucket.getAvailableTokens().get() == 60 // because should be just reduced to maximum
                }
            }
        }
        where:
        bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType test replace configuration proportionally from gready refill to gready refill. Case for roundingError propogation"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose: [true, false]) {
                // System.err.println("sync: $sync verbose: $verbose")
                TimeMeterMock clock = new TimeMeterMock(0)
                BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(6, Duration.ofNanos(8)).withInitialTokens(0))
                    .build()
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(3, Duration.ofNanos(8)))
                    .build()
                if (sync) {
                    Bucket bucket = bucketType.createBucket(configuration, clock) // 0.75
                    bucket.getAvailableTokens()
                    clock.addTime(3) // 2.25
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY)
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY)
                    }
                    assert bucket.getAvailableTokens() == 1 // 1.125 after replacement
                    assert bucket.asVerbose().getAvailableTokens().getState().getRoundingError(0) == 1 // 1/8 == 0.125
                } else {
                    AsyncBucketProxy bucket = bucketType.createAsyncBucket(configuration, clock) // 0.75
                    bucket.getAvailableTokens().get()
                    clock.addTime(3) // 2.25
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get()
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get()
                    }
                    assert bucket.getAvailableTokens().get() == 1 // 1.125 after replacement
                    assert bucket.asVerbose().getAvailableTokens().get().state.getRoundingError(0) == 1 // 1/8 == 0.125
                }
            }
        }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType test replace configuration proportionally from gready refill to gready refill. Case for roundingError propogation and negative amount"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                for (boolean verbose: [true, false]) {
                    // System.err.println("sync: $sync verbose: $verbose")
                    TimeMeterMock clock = new TimeMeterMock(0)
                    def configuration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(6, Duration.ofNanos(8)).withInitialTokens(0))
                        .build()
                    BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(8)))
                        .build()
                    if (sync) {
                        Bucket bucket = bucketType.createBucket(configuration, clock) // 0.75
                        bucket.getAvailableTokens()
                        clock.addTime(3) // 2.25
                        bucket.consumeIgnoringRateLimits(5) // -2.75
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY)
                        }
                        assert bucket.getAvailableTokens() == -2 // -1.375 after replacement
                        assert bucket.asVerbose().getAvailableTokens().getState().getRoundingError(0) == 5 // 5/8 == 1 - 0.375
                    } else {
                        def bucket = bucketType.createAsyncBucket(configuration, clock) // 0.75
                        bucket.getAvailableTokens().get()
                        clock.addTime(3) // 2.25
                        bucket.consumeIgnoringRateLimits(5).get() // -2.75
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get()
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensInheritanceStrategy.PROPORTIONALLY).get()
                        }
                        assert bucket.getAvailableTokens().get() == -2 // -1.375 after replacement
                        assert bucket.asVerbose().getAvailableTokens().get().state.getRoundingError(0) == 5 // 5/8 == 1 - 0.375
                    }
                }
            }
        where:
            bucketType << BucketType.values()
    }

}
