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

import io.github.bucket4j.local.LocalBucketBuilder
import io.github.bucket4j.mock.BackendMock
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.remote.BucketProxy
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

import static io.github.bucket4j.BucketExceptions.*
import static io.github.bucket4j.remote.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION

class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final Duration VALID_PERIOD = Duration.ofMinutes(10)
    private static final long VALID_CAPACITY = 1000

    AbstractBucketBuilder builder = Bucket4j.builder()

    @Unroll
    def "Should detect that capacity #capacity is wrong"(long capacity) {
        when:
            builder.addLimit(Bandwidth.classic(capacity, Refill.greedy(1, VALID_PERIOD)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveCapacity(capacity).message

        where:
            capacity << [-10, -5, 0]
    }

    @Unroll
    def "Should detect that initialTokens #initialTokens is wrong"(long initialTokens) {
        when:
            Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
                    .withInitialTokens(initialTokens)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveInitialTokens(initialTokens).message
        where:
            initialTokens << [-10, -1]
    }

    @Unroll
    def "Should check that #period is invalid period of bandwidth"(long period) {
        when:
            builder.addLimit(Bandwidth.simple(VALID_CAPACITY, Duration.ofMinutes(period)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriod(Duration.ofMinutes(period).toNanos()).message

        where:
            period << [-10, -1, 0]
    }

    def "Should check that refill is not null"() {
        when:
            builder.addLimit(Bandwidth.classic(42, null))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullBandwidthRefill().message
    }

    def "Should check that bandwidth is not null"() {
        when:
            builder.addLimit(null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullBandwidth().message
    }

    def "Should check that refill period is not null"() {
        when:
            builder.addLimit(Bandwidth.classic( 32, Refill.greedy(1, null)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullRefillPeriod().message
    }

    @Unroll
    def "Should detect that refill #refillTokens tokens is invalid"(int refillTokens) {
        when:
            builder.addLimit(Bandwidth.classic( 32, Refill.greedy(refillTokens, Duration.ofSeconds(1))))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriodTokens(refillTokens).message
        where:
            refillTokens << [0, -2]
    }

    def "Should check that time meter is not null"() {
        when:
            Bucket4j.builder().withCustomTimePrecision(null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullTimeMeter().message
    }

    @Unroll
    def "Should check that listener is not null when decorating bucket with type #bucketType"(BucketType bucketType) {
        when:
            bucketType.createBucket(Bucket4j.builder().addLimit(Bandwidth.simple(3, Duration.ofMinutes(1))))
                    .toListenable(null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullListener().message
        where:
            bucketType << BucketType.values()
    }

    def  "Should check that limited bandwidth list is not empty"() {
        setup:
            def builder = Bucket4j.builder()
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == restrictionsNotSpecified().message
    }

    def "Should check that tokens to consume should be positive"() {
        setup:
            def bucket = Bucket4j.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()

        when:
            bucket.tryConsume(0)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.tryConsume(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.tryConsumeAsMuchAsPossible(0)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.tryConsumeAsMuchAsPossible(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.asScheduler().tryConsume(0L, VALID_PERIOD.toNanos(), BlockingStrategy.PARKING)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.asScheduler().tryConsume(-1, VALID_PERIOD.toNanos(), BlockingStrategy.PARKING)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message
    }

    def "Should detect the high rate of refill"() {
        when:
           Bucket4j.builder().addLimit(Bandwidth.simple(2, Duration.ofNanos(1)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == tooHighRefillRate(1, 2).message
    }

    def "Should check that time units to wait should be positive"() {
        setup:
            def bucket = Bucket4j.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()
        when:
            bucket.asScheduler().tryConsume(1, 0, BlockingStrategy.PARKING)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveNanosToWait(0).message

        when:
            bucket.asScheduler().tryConsume(1, -1, BlockingStrategy.PARKING)
        then:
            ex = thrown()
            ex.message == nonPositiveNanosToWait(-1).message
    }

    @Unroll
    def "Should check that #tokens tokens is not positive to add"(long tokens) {
        setup:
            def bucket = Bucket4j.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()
        when:
            bucket.addTokens(tokens)
        then:
            thrown(IllegalArgumentException)
        where:
            tokens << [0, -1, -10]
    }

    def "Should that scheduler passed to tryConsume is not null"() {
        setup:
            def bucket = Bucket4j.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()
        when:
            bucket.asAsyncScheduler().tryConsume(32, 1000_000, null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullScheduler().message
    }

    def "GridBucket should check that configuration is not null"() {
        setup:
            BackendMock mockProxy = new BackendMock(TimeMeter.SYSTEM_MILLISECONDS)
        when:
            BucketProxy.createInitializedBucket("66", null, mockProxy, THROW_BUCKET_NOT_FOUND_EXCEPTION)

        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullConfiguration().message

        when:
            BucketProxy.createLazyBucket("66", {null}, mockProxy)
                    .tryConsume(1)
        then:
            ex = thrown()
            ex.message == nullConfiguration().message

        when:
            BucketProxy.createLazyBucket("66", null, mockProxy)
                    .tryConsume(1)
        then:
            ex = thrown()
            ex.message == nullConfigurationSupplier().message
    }

}
