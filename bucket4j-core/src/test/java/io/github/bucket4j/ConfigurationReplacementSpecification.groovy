
package io.github.bucket4j

import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class ConfigurationReplacementSpecification extends Specification {

    @Unroll
    def "#bucketType should perform refill before replace configuration"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                TimeMeterMock clock = new TimeMeterMock(0)
                Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                        .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)),
                        clock
                )
                clock.addTime(10)
                BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                        .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)))
                        .build()
                if (sync) {
                    bucket.replaceConfiguration(newConfiguration)
                } else {
                    bucket.asAsync().replaceConfiguration(newConfiguration).get()
                }
                assert bucket.getAvailableTokens() == 10
            }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should decrease available tokens when reducing capacity"(BucketType bucketType) {
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
                bucket.replaceConfiguration(newConfiguration)
            } else {
                bucket.asAsync().replaceConfiguration(newConfiguration).get()
            }
            assert bucket.getAvailableTokens() == 200
        }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should apply new configuration"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            TimeMeterMock clock = new TimeMeterMock(0)
            Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                    .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)),
                    clock
            )
            BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)))
                    .build()
            if (sync) {
                bucket.replaceConfiguration(newConfiguration)
            } else {
                bucket.asAsync().replaceConfiguration(newConfiguration).get()
            }
            clock.addTime(10)
            assert bucket.getAvailableTokens() == 1
        }
        where:
            bucketType << BucketType.values()
    }

}
