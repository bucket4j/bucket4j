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

import io.github.bucket4j.mock.BackendMock
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import io.github.bucket4j.remote.BucketProxy
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS
import static io.github.bucket4j.remote.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION

class BucketSpecification extends Specification {

    @Unroll
    def "#n Should return #requiredResult when trying to consume single token from Bucket #builder"(
            int n, boolean requiredResult, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    def timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    if (sync) {
                        assert bucket.tryConsume(1) == requiredResult
                    } else {
                        assert bucket.asAsync().tryConsume(1).get() == requiredResult
                    }
                }
            }
        where:
            n | requiredResult |  builder
            1 |     false      |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0))
            2 |      true      |  Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(1))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consume #toConsume tokens from Bucket #builder"(
            int n, boolean requiredResult, long toConsume, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    def timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    if (sync) {
                        assert bucket.tryConsume(toConsume) == requiredResult
                    } else {
                        assert bucket.asAsync().tryConsume(toConsume).get() == requiredResult
                    }
                }
            }
        where:
            n | requiredResult | toConsume | builder
            1 |     false      |     1     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0))
            2 |      true      |     1     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(1))
    }

    @Unroll
    def "#n tryConsumeAndReturnRemaining specification"(int n, long toConsume, boolean result, long expectedRemaining, long expectedWait, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    ConsumptionProbe probe
                    if (sync) {
                        probe = bucket.tryConsumeAndReturnRemaining(toConsume)
                    } else {
                        probe = bucket.asAsync().tryConsumeAndReturnRemaining(toConsume).get()
                    }
                    assert probe.consumed == result
                    assert probe.remainingTokens == expectedRemaining
                    assert probe.nanosToWaitForRefill == expectedWait
                }
            }
        where:
            n | toConsume | result  |  expectedRemaining | expectedWait | builder
            1 |    49     |   true  |           51       |       0      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(100))
            2 |     1     |   true  |            0       |       0      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
            3 |    80     |   false |           70       |      10      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70))
            4 |    10     |   false |            0       |      10      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            5 |   120     |   false |           10       |     110      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(10))
    }

    @Unroll
    def "#n Should return #requiredResult when consumeAsMuchAsPossible tokens from Bucket #builder"(
            int n, long requiredResult, AbstractBucketBuilder builder) {
        expect:
            for (BucketType bucketType : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = bucketType.createBucket(builder, timeMeter)
                    if (sync) {
                        assert bucket.tryConsumeAsMuchAsPossible() == requiredResult
                    } else {
                        assert bucket.asAsync().tryConsumeAsMuchAsPossible().get() == requiredResult
                    }
                }
            }
        where:
            n | requiredResult | builder
            1 |        0       | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0))
            2 |        2       | Bucket4j.builder().withCustomTimePrecision(new TimeMeterMock(0)).addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(2))
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consumeAsMuchAsPossible with limit #limit tokens from Bucket #builder"(
            int n, long requiredResult, long limit, AbstractBucketBuilder builder) {
        expect:
            for (BucketType bucketType : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = bucketType.createBucket(builder, timeMeter)
                    if (sync) {
                        assert bucket.tryConsumeAsMuchAsPossible(limit) == requiredResult
                    } else {
                        assert bucket.asAsync().tryConsumeAsMuchAsPossible(limit).get() == requiredResult
                    }
                }
            }
        where:
            n | requiredResult |   limit   | builder
            1 |       4        |     5     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(4))
            2 |       5        |     5     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5))
            3 |       5        |     5     | Bucket4j.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5))
    }

    @Unroll
    def "#n Add tokens spec"(
            int n, long tokensToAdd, long nanosIncrement, long requiredResult, AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                for (boolean sync : [true, false]) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(builder, timeMeter)
                    timeMeter.addTime(nanosIncrement)
                    if (sync) {
                        bucket.addTokens(tokensToAdd)
                    } else {
                        bucket.asAsync().addTokens(tokensToAdd).get()
                    }
                    assert bucket.createSnapshot().getAvailableTokens(bucket.configuration.bandwidths) == requiredResult
                }
            }
        where:
            n | tokensToAdd | nanosIncrement | requiredResult | builder
            1 |     49      |     50         |        99      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            2 |     50      |     50         |       100      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            3 |     50      |     0          |        50      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            4 |     120     |     0          |       100      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            5 |     120     |     110        |       100      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
    }

    @Unroll
    def "#n getAvailableTokens specification"(int n, long nanosSinceBucketCreation, long expectedTokens,  AbstractBucketBuilder builder) {
        expect:
            for (BucketType type : BucketType.values()) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(builder, timeMeter)
                timeMeter.addTime(nanosSinceBucketCreation)
                assert bucket.getAvailableTokens() == expectedTokens
            }
        where:
            n | nanosSinceBucketCreation | expectedTokens |  builder
            1 |             49           |     50         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
            2 |             50           |     50         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            3 |             50           |    100         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70))
            4 |              0           |      0         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
            5 |            120           |    100         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
    }

    def "should complete future exceptionally if backend failed"() {
        setup:
            BackendMock mockProxy = new BackendMock(SYSTEM_MILLISECONDS);
            BucketConfiguration configuration = Bucket4j.configurationBuilder(mockProxy)
                                                    .addLimit(Bandwidth.simple(1, Duration.ofNanos(1)))
                                                    .build()

            Bucket bucket = BucketProxy.createInitializedBucket("66", configuration, mockProxy, THROW_BUCKET_NOT_FOUND_EXCEPTION)
        when:
            mockProxy.setException(new RuntimeException())
            CompletableFuture<Boolean> future = bucket.asAsync().tryConsume(1)
        then:
            future.isCompletedExceptionally()
    }

    def "check that toString does not fail"() {
        when:
            for (BucketType type : BucketType.values()) {
                for (TimeMeter meter : [SYSTEM_MILLISECONDS, SYSTEM_MILLISECONDS]) {
                    AbstractBucketBuilder builder = Bucket4j.builder()
                            .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
                    Bucket bucket = type.createBucket(builder, meter)
                    println bucket.toString()
                }
            }
        then:
            noExceptionThrown()
    }

}
