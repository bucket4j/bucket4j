
package io.github.bucket4j.api_specifications.regular

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.IncompatibleConfigurationException
import io.github.bucket4j.Refill
import io.github.bucket4j.distributed.AsyncBucket
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import io.github.bucket4j.util.ComparableByContent
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.ExecutionException

import static io.github.bucket4j.util.PackageAccessor.getState
import static junit.framework.Assert.assertNotSame

class ConfigurationReplacementSpecification extends Specification {

    @Unroll
    def "#bucketType should prevent increasing count of bandwidths"(BucketType bucketType) {
        setup:
        BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)))
                    .build()
        Bucket bucket = bucketType.createBucket(configuration)
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                    .addLimit(Bandwidth.simple(1000, Duration.ofHours(1)))
                    .build()

        when:
            bucket.replaceConfiguration(newConfiguration)
        then:
        IncompatibleConfigurationException ex = thrown(IncompatibleConfigurationException)
            ComparableByContent.equals(ex.newConfiguration, newConfiguration)
            ComparableByContent.equals(ex.previousConfiguration, bucket.getConfiguration())

        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should prevent increasing count of bandwidths when replacing configuration async"(BucketType bucketType) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)))
                    .build()
            AsyncBucket bucket = bucketType.createAsyncBucket(configuration)
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(1000, Duration.ofHours(1)))
                .build()

        when:
            bucket.replaceConfiguration(newConfiguration).get()
        then:
            ExecutionException executionException = thrown(ExecutionException)
            IncompatibleConfigurationException asyncException = executionException.getCause()
            ComparableByContent.equals(asyncException.newConfiguration, newConfiguration)
            ComparableByContent.equals(asyncException.previousConfiguration, configuration)
        where:
            bucketType << BucketType.withAsyncSupport()
    }

    @Unroll
    def "#bucketType should prevent decreasing count of bandwidths"(BucketType bucketType) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                    .addLimit(Bandwidth.simple(1000, Duration.ofHours(1)))
                    .build()

            Bucket bucket = bucketType.createBucket(configuration)
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)))
                    .build()

        when:
            bucket.replaceConfiguration(newConfiguration)
        then:
            IncompatibleConfigurationException ex = thrown(IncompatibleConfigurationException)
            ComparableByContent.equals(ex.newConfiguration, newConfiguration)
            ComparableByContent.equals(ex.previousConfiguration, configuration)

        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should prevent decreasing count of bandwidths when replacing configuration async"(BucketType bucketType) {
        setup:
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                    .addLimit(Bandwidth.simple(1000, Duration.ofHours(1)))
                    .build()
            AsyncBucket bucket = bucketType.createAsyncBucket(configuration)
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)))
                    .build()

        when:
            bucket.replaceConfiguration(newConfiguration).get()
        then:
            ExecutionException executionException = thrown(ExecutionException)
            IncompatibleConfigurationException asyncException = executionException.getCause()
            ComparableByContent.equals(asyncException.newConfiguration, newConfiguration)
            ComparableByContent.equals(asyncException.previousConfiguration, configuration)
        where:
            bucketType << BucketType.withAsyncSupport()
    }

    @Unroll
    def "#bucketType should perform refill before replace configuration"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                TimeMeterMock clock = new TimeMeterMock(0)
                def configuration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
                        .build()
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)))
                        .build()
                if (sync || !bucketType.isAsyncModeSupported()) {
                    Bucket bucket = bucketType.createBucket(configuration, clock)
                    clock.addTime(10)
                    bucket.replaceConfiguration(newConfiguration)
                    bucket.getAvailableTokens() == 10
                } else {
                    AsyncBucket bucket = bucketType.createAsyncBucket(configuration, clock)
                    clock.addTime(10)
                    bucket.replaceConfiguration(newConfiguration).get()
                    bucket.getAvailableTokens().get() == 10
                }
            }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should decrease available tokens when reducing capacity"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            TimeMeterMock clock = new TimeMeterMock(0)
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(500, Refill.greedy(100, Duration.ofNanos(100))))
                    .build()
            BucketConfiguration newConfiguration = BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic (200, Refill.greedy(100, Duration.ofNanos(100)) ))
                    .build()
            if (sync || !bucketType.isAsyncModeSupported()) {
                Bucket bucket = bucketType.createBucket(configuration, clock)
                bucket.replaceConfiguration(newConfiguration)
                bucket.getAvailableTokens() == 200
            } else {
                AsyncBucket bucket = bucketType.createAsyncBucket(configuration, clock)
                bucket.replaceConfiguration(newConfiguration).get()
                bucket.getAvailableTokens().get() == 200
            }
        }
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should apply new configuration"(BucketType bucketType) {
        expect:
        for (boolean sync : [true, false]) {
            for (boolean verbose : [true, false]) {
                TimeMeterMock clock = new TimeMeterMock(0)
                def configuration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
                        .build()
                BucketConfiguration newConfiguration = BucketConfiguration.builder()
                        .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)))
                        .build()
                if (sync || !bucketType.isAsyncModeSupported()) {
                    Bucket bucket = bucketType.createBucket(configuration, clock)
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration)
                    } else {
                        def verboseResult = bucket.asVerbose().replaceConfiguration(newConfiguration)
                        if (bucketType.isLocal()) {
                            assertNotSame(verboseResult.state, getState(bucket))
                        }
                    }
                    clock.addTime(10)
                    assert bucket.getAvailableTokens() == 1
                } else {
                    AsyncBucket bucket = bucketType.createAsyncBucket(configuration, clock)
                    if (!verbose) {
                        bucket.replaceConfiguration(newConfiguration).get()
                    } else {
                        def verboseResult = bucket.asVerbose().replaceConfiguration(newConfiguration).get()
                        if (bucketType.isLocal()) {
                            assertNotSame(verboseResult.state, getState(bucket))
                        }
                    }
                    clock.addTime(10)
                    assert bucket.getAvailableTokens().get() == 1
                }
            }
        }
        where:
            bucketType << BucketType.values()
    }

}
