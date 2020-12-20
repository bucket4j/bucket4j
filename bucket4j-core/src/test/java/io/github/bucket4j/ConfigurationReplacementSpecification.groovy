
package io.github.bucket4j

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
                // System.err.println("sync: $sync verbose: $verbose")
                TimeMeterMock clock = new TimeMeterMock(0)
                Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0)),
                        clock
                )
                clock.addTime(3) // 1.8
                BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build()
                if (sync) {
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.RESET)
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.RESET)
                    }
                } else {
                    if (!verbose) {
                        bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.RESET).get()
                    } else {
                        bucket.asAsync().asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.RESET).get()
                    }
                }
                assert bucket.getAvailableTokens() == 60
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
                    Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0).withId("x")),
                            clock
                    )
                    clock.addTime(3) // 1.8
                    BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                            .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)).withId("y"))
                            .build()
                    if (sync) {
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS)
                        }
                    } else {
                        if (!verbose) {
                            bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.RESET).get()
                        } else {
                            bucket.asAsync().asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.RESET).get()
                        }
                    }
                    assert bucket.getAvailableTokens() == 60
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
                    Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0).withId("x"))
                            .addLimit(Bandwidth.simple(1000, Duration.ofNanos(1000)).withInitialTokens(1).withId("z")),
                            clock
                    )
                    BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                            .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)).withId("y"))
                            .addLimit(Bandwidth.simple(1000, Duration.ofNanos(1000)).withId("z"))
                            .build()
                    if (sync) {
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS)
                        }
                    } else {
                        if (!verbose) {
                            bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS).get()
                        } else {
                            bucket.asAsync().asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS).get()
                        }
                    }
                    assert bucket.getAvailableTokens() == 1
                    def snapshot = bucket.createSnapshot()
                    assert snapshot.getCurrentSize(0) == 60
                    assert snapshot.getCurrentSize(1) == 1
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
                    // System.err.println("sync: $sync verbose: $verbose")
                    TimeMeterMock clock = new TimeMeterMock(0)
                    Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0)),
                            clock
                    )
                    clock.addTime(3) // 1.8
                    BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                            .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                            .build()
                    if (sync) {
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS)
                        }
                    } else {
                        if (!verbose) {
                            bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS).get()
                        } else {
                            bucket.asAsync().asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS).get()
                        }
                    }
                    assert bucket.getAvailableTokens() == 1

                    clock.addTime(4)
                    assert bucket.getAvailableTokens() == 2
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
                    Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                            .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0)),
                            clock
                    )
                    clock.addTime(3) // 1.8
                    Refill refill = Refill.intervally(60, Duration.ofNanos(1000))
                    BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                            .addLimit(Bandwidth.classic(60, refill))
                            .build()
                    if (sync) {
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS)
                        }
                    } else {
                        if (!verbose) {
                            bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS).get()
                        } else {
                            bucket.asAsync().asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS).get()
                        }
                    }
                    assert bucket.getAvailableTokens() == 1

                    clock.addTime(999)
                    assert bucket.getAvailableTokens() == 1 // 0.8 tokens from previous bucket should not be copied after config replacement

                    clock.addTime(1)
                    assert bucket.getAvailableTokens() == 60
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
            Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                    .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100)))),
                    clock
            )
            BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                    .addLimit(Bandwidth.classic (200, Refill.greedy(100, Duration.ofNanos(100)) ))
                    .build()
            if (sync) {
                bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS)
            } else {
                bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.AS_IS).get()
            }
            assert bucket.getAvailableTokens() == 200
        }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should decrease available tokens when reducing capacity and copying tokens proportionally"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                TimeMeterMock clock = new TimeMeterMock(0)
                Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                        .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100)))),
                        clock
                )
                BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                        .addLimit(Bandwidth.classic (200, Refill.greedy(100, Duration.ofNanos(100)) ))
                        .build()
                if (sync) {
                    bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY)
                } else {
                    bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY).get()
                }
                assert bucket.getAvailableTokens() == 200
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
                Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(5)).withInitialTokens(0)),
                        clock
                )
                clock.addTime(3) // 1.8
                BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                        .addLimit(Bandwidth.simple(60, Duration.ofNanos(1000)))
                        .build()
                if (sync) {
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY)
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY)
                    }
                } else {
                    if (!verbose) {
                        bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY).get()
                    } else {
                        bucket.asAsync().asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY).get()
                    }
                }
                assert bucket.getAvailableTokens() == 36

                clock.addTime(4)
                assert bucket.getAvailableTokens() == 36

//                def snapshot = bucket.createSnapshot()
//                System.err.println("bucketType: $bucketType, sync: $sync verbose: $verbose availableTokens: ${snapshot.getCurrentSize(0)}  roundingError: ${snapshot.getRoundingError(0)}")

                clock.addTime(13)
                assert bucket.getAvailableTokens() == 37
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
                Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                        .addLimit(Bandwidth.simple(6, Duration.ofNanos(8)).withInitialTokens(0)), // 0.75
                        clock
                )
                clock.addTime(3) // 2.25
                BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                        .addLimit(Bandwidth.simple(3, Duration.ofNanos(8)))
                        .build()
                if (sync) {
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY)
                    } else {
                        bucket.asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY)
                    }
                } else {
                    if (!verbose) {
                        bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY).get()
                    } else {
                        bucket.asAsync().asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY).get()
                    }
                }
                assert bucket.getAvailableTokens() == 1 // 1.125 after replacement
                assert bucket.createSnapshot().getRoundingError(0) == 1 // 1/8 == 0.125
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
                    Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                            .addLimit(Bandwidth.simple(6, Duration.ofNanos(8)).withInitialTokens(0)), // 0.75
                            clock
                    )
                    clock.addTime(3) // 2.25
                    bucket.consumeIgnoringRateLimits(5) // -2.75
                    BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                            .addLimit(Bandwidth.simple(3, Duration.ofNanos(8)))
                            .build()
                    if (sync) {
                        if (!verbose) {
                            bucket.replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY)
                        } else {
                            bucket.asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY)
                        }
                    } else {
                        if (!verbose) {
                            bucket.asAsync().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY).get()
                        } else {
                            bucket.asAsync().asVerbose().replaceConfiguration(newConfiguration, TokensMigrationMode.PROPORTIONALLY).get()
                        }
                    }
                    assert bucket.getAvailableTokens() == -2 // -1.375 after replacement
                    assert bucket.createSnapshot().getRoundingError(0) == 5 // 5/8 == 1 - 0.375
                }
            }
        where:
            bucketType << BucketType.values()
    }

}
