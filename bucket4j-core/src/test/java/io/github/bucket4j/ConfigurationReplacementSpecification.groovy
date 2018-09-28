/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j

import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.ExecutionException

import static EqualsUtil.isConfigEquals

class ConfigurationReplacementSpecification extends Specification {

    @Unroll
    def "#bucketType should prevent increasing count of bandwidths"(BucketType bucketType) {
        setup:
            Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                    .addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)))
            )
            BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                    .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                    .addLimit(Bandwidth.simple(1000, Duration.ofHours(1)))
                    .build()

        when:
            bucket.replaceConfiguration(newConfiguration)
        then:
            IncompatibleConfigurationException ex = thrown(IncompatibleConfigurationException)
            isConfigEquals(ex.newConfiguration, newConfiguration)
            isConfigEquals(ex.previousConfiguration, bucket.getConfiguration())

        when:
            bucket.asAsync().replaceConfiguration(newConfiguration).get()
        then:
            ExecutionException executionException = thrown(ExecutionException)
            IncompatibleConfigurationException asyncException = executionException.getCause()
            isConfigEquals(asyncException.newConfiguration, newConfiguration)
            isConfigEquals(asyncException.previousConfiguration, bucket.getConfiguration())
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should prevent decreasing count of bandwidths"(BucketType bucketType) {
        setup:
            Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                    .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                    .addLimit(Bandwidth.simple(1000, Duration.ofHours(1)))
            )
            BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                    .addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)))
                    .build()

        when:
            bucket.replaceConfiguration(newConfiguration)
            then:
            IncompatibleConfigurationException ex = thrown(IncompatibleConfigurationException)
            isConfigEquals(ex.newConfiguration, newConfiguration)
            isConfigEquals(ex.previousConfiguration, bucket.getConfiguration())

        when:
            bucket.asAsync().replaceConfiguration(newConfiguration).get()
        then:
            ExecutionException executionException = thrown(ExecutionException)
            IncompatibleConfigurationException asyncException = executionException.getCause()
            isConfigEquals(asyncException.newConfiguration, newConfiguration)
            isConfigEquals(asyncException.previousConfiguration, bucket.getConfiguration())
        where:
            bucketType << BucketType.values()
    }

    @Unroll
    def "#bucketType should perform refill before replace configuration"(BucketType bucketType) {
        expect:
            for (boolean sync : [true, false]) {
                TimeMeterMock clock = new TimeMeterMock(0)
                Bucket bucket = bucketType.createBucket(Bucket4j.builder()
                        .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
                        .withCustomTimePrecision(clock)
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
                bucket.getAvailableTokens() == 10
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
                    .addLimit(Bandwidth.classic (500, Refill.greedy(100, Duration.ofNanos(100)) ))
                    .withCustomTimePrecision(clock)
            )
            BucketConfiguration newConfiguration = Bucket4j.configurationBuilder()
                    .addLimit(Bandwidth.classic (200, Refill.greedy(100, Duration.ofNanos(100)) ))
                    .build()
            if (sync) {
                bucket.replaceConfiguration(newConfiguration)
            } else {
                bucket.asAsync().replaceConfiguration(newConfiguration).get()
            }
            bucket.getAvailableTokens() == 200
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
                    .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
                    .withCustomTimePrecision(clock)
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
            bucket.getAvailableTokens() == 1
        }
        where:
            bucketType << BucketType.values()
    }

}
