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

import io.github.bucket4j.distributed.AsyncBucket
import io.github.bucket4j.mock.GridBackendMock
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture

import static io.github.bucket4j.TimeMeter.SYSTEM_MILLISECONDS
import static io.github.bucket4j.distributed.proxy.RecoveryStrategy.THROW_BUCKET_NOT_FOUND_EXCEPTION

class BucketSpecification extends Specification {

    @Unroll
    def "#n Should return #requiredResult when trying to consume single token from Bucket #configuration"(
            int n, boolean requiredResult, BucketConfiguration configuration) {
        expect:
            for (BucketType type : BucketType.values()) {
                def timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(configuration, timeMeter)
                assert bucket.tryConsume(1) == requiredResult
                if (type.isAsyncModeSupported()) {
                    AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                    assert asyncBucket.tryConsume(1).get() == requiredResult
                }
            }
        where:
            n | requiredResult |  configuration
            1 |     false      |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0)).build()
            2 |      true      |  BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(1)).build()
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consume #toConsume tokens from Bucket #builder"(
            int n, boolean requiredResult, long toConsume, BucketConfiguration configuration) {
        expect:
            for (BucketType type : BucketType.values()) {
                def timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(configuration, timeMeter)
                assert bucket.tryConsume(toConsume) == requiredResult
                if (type.isAsyncModeSupported()) {
                    AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                    assert asyncBucket.tryConsume(toConsume).get() == requiredResult
                }
            }
        where:
            n | requiredResult | toConsume | configuration
            1 |     false      |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0)).build()
            2 |      true      |     1     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(1)).build()
    }

    @Unroll
    def "#n tryConsumeAndReturnRemaining specification"(int n, long toConsume, boolean result, long expectedRemaining, long expectedWait, BucketConfiguration configuration) {
        expect:
            for (BucketType type : BucketType.values()) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(configuration, timeMeter)
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(toConsume)
                assert probe.consumed == result
                assert probe.remainingTokens == expectedRemaining
                assert probe.nanosToWaitForRefill == expectedWait
                if (type.isAsyncModeSupported()) {
                    AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                    probe = asyncBucket.tryConsumeAndReturnRemaining(toConsume).get()
                    assert probe.consumed == result
                    assert probe.remainingTokens == expectedRemaining
                    assert probe.nanosToWaitForRefill == expectedWait
                }
            }
        where:
            n | toConsume | result  |  expectedRemaining | expectedWait | configuration
            1 |    49     |   true  |           51       |       0      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(100)).build()
            2 |     1     |   true  |            0       |       0      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1)).build()
            3 |    80     |   false |           70       |      10      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70)).build()
            4 |    10     |   false |            0       |      10      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
            5 |   120     |   false |           10       |     110      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(10)).build()
    }

    @Unroll
    def "#n tryConsumeAndReturnRemaining specification"(int n, long toEstimate, boolean result, long expectedWait, BucketConfiguration configuration) {
        expect:
            for (BucketType type : BucketType.values()) {
                    TimeMeterMock timeMeter = new TimeMeterMock(0)
                    Bucket bucket = type.createBucket(configuration, timeMeter)
                    long availableTokensBeforeEstimation = bucket.getAvailableTokens()
                    EstimationProbe probe = bucket.estimateAbilityToConsume(toEstimate)
                    assert probe.canBeConsumed() == result
                    assert probe.remainingTokens == availableTokensBeforeEstimation
                    assert probe.nanosToWaitForRefill == expectedWait
                    assert bucket.getAvailableTokens() == availableTokensBeforeEstimation

                    if (type.isAsyncModeSupported()) {
                        AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                        availableTokensBeforeEstimation = bucket.getAvailableTokens()
                        probe = asyncBucket.estimateAbilityToConsume(toEstimate).get()
                        assert probe.canBeConsumed() == result
                        assert probe.remainingTokens == availableTokensBeforeEstimation
                        assert probe.nanosToWaitForRefill == expectedWait
                        assert bucket.getAvailableTokens() == availableTokensBeforeEstimation
                    }
            }
        where:
            n | toEstimate | result  |  expectedWait | configuration
            1 |    49      |   true  |        0      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(100)).build()
            2 |     1      |   true  |        0      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1)).build()
            3 |    80      |   false |       10      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70)).build()
            4 |    10      |   false |       10      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
            5 |   120      |   false |      110      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(10)).build()
            6 |    80      |   false |      100      | BucketConfiguration.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofNanos(100))).withInitialTokens(70)).build()
            7 |    10      |   false |      100      | BucketConfiguration.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofNanos(100))).withInitialTokens(0)).build()
            8 |   120      |   false |      200      | BucketConfiguration.builder().addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofNanos(100))).withInitialTokens(10)).build()
    }

    @Unroll
    def "#n Should return #requiredResult when consumeAsMuchAsPossible tokens from Bucket #builder"(int n, long requiredResult, BucketConfiguration configuration) {
        expect:
            for (BucketType bucketType : BucketType.values()) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = bucketType.createBucket(configuration, timeMeter)
                assert bucket.tryConsumeAsMuchAsPossible() == requiredResult
                if (bucketType.isAsyncModeSupported()) {
                    AsyncBucket asyncBucket = bucketType.createAsyncBucket(configuration, timeMeter)
                    assert asyncBucket.tryConsumeAsMuchAsPossible().get() == requiredResult
                }
            }
        where:
            n | requiredResult | configuration
            1 |        0       | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(0)).build()
            2 |        2       | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(2)).build()
    }

    @Unroll
    def "#n Should return #requiredResult when trying to consumeAsMuchAsPossible with limit #limit tokens from Bucket #configuration"(
            int n, long requiredResult, long limit, BucketConfiguration configuration) {
        expect:
            for (BucketType bucketType : BucketType.values()) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = bucketType.createBucket(configuration, timeMeter)
                assert bucket.tryConsumeAsMuchAsPossible(limit) == requiredResult
                if (bucketType.isAsyncModeSupported()) {
                    AsyncBucket asyncBucket = bucketType.createAsyncBucket(configuration, timeMeter)
                    assert asyncBucket.tryConsumeAsMuchAsPossible(limit).get() == requiredResult
                }
            }
        where:
            n | requiredResult |   limit   | configuration
            1 |       4        |     5     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(4)).build()
            2 |       5        |     5     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5)).build()
            3 |       5        |     5     | BucketConfiguration.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(100)).withInitialTokens(5)).build()
    }

    @Unroll
    def "#n Add tokens spec"(
            int n, long tokensToAdd, long nanosIncrement, long requiredResult, BucketConfiguration configuration) {
        expect:
            for (BucketType type : BucketType.values()) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(configuration, timeMeter)
                timeMeter.addTime(nanosIncrement)
                bucket.addTokens(tokensToAdd)
                assert bucket.getAvailableTokens() == requiredResult

                if (type.isAsyncModeSupported()) {
                    timeMeter = new TimeMeterMock(0)
                    AsyncBucket asyncBucket = type.createAsyncBucket(configuration, timeMeter)
                    asyncBucket.addTokens(tokensToAdd).get()
                    assert asyncBucket.getAvailableTokens().get() == requiredResult
                }
            }
        where:
            n | tokensToAdd | nanosIncrement | requiredResult | configuration
            1 |     49      |     50         |        99      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
            2 |     50      |     50         |       100      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
            3 |     50      |     0          |        50      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
            4 |     120     |     0          |       100      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
            5 |     120     |     110        |       100      | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
    }

    @Unroll
    def "#n getAvailableTokens specification"(int n, long nanosSinceBucketCreation, long expectedTokens, BucketConfiguration configuration) {
        expect:
            for (BucketType type : BucketType.values()) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(configuration, timeMeter)
                timeMeter.addTime(nanosSinceBucketCreation)
                assert bucket.getAvailableTokens() == expectedTokens
            }
        where:
            n | nanosSinceBucketCreation | expectedTokens |  configuration
            1 |             49           |     50         | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1)).build()
            2 |             50           |     50         | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
            3 |             50           |    100         | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(70)).build()
            4 |              0           |      0         | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
            5 |            120           |    100         | BucketConfiguration.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0)).build()
    }

    def "should complete future exceptionally if backend failed"() {
        setup:
            GridBackendMock backendMock = new GridBackendMock(SYSTEM_MILLISECONDS)
            BucketConfiguration configuration = BucketConfiguration.builder()
                                                    .addLimit(Bandwidth.simple(1, Duration.ofNanos(1)))
                                                    .build()

            AsyncBucket bucket = backendMock.builder()
                    .withRecoveryStrategy(THROW_BUCKET_NOT_FOUND_EXCEPTION)
                    .buildAsyncProxy("66", configuration)
        when:
            backendMock.setException(new RuntimeException())
            CompletableFuture<Boolean> future = bucket.tryConsume(1)
        then:
            future.isCompletedExceptionally()
    }

    def "check that toString does not fail"() {
        when:
            for (BucketType type : BucketType.values()) {
                for (TimeMeter meter : [SYSTEM_MILLISECONDS, SYSTEM_MILLISECONDS]) {
                    BucketConfiguration configuration = BucketConfiguration.builder()
                            .addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(1))
                            .build()
                    Bucket bucket = type.createBucket(configuration, meter)
                    println bucket.toString()
                    if (type.isAsyncModeSupported()) {
                        AsyncBucket asyncBucket = type.createAsyncBucket(configuration, meter)
                        println asyncBucket.toString()
                    }
                }
            }
        then:
            noExceptionThrown()
    }

}
