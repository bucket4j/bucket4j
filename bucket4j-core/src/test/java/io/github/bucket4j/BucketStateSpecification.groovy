/*
 *
 *   Copyright 2015-2017 Vladimir Bukhtoyarov
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.bucket4j

import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration


class BucketStateSpecification extends Specification {

    @Unroll
    def "GetAvailableTokens specification #testNumber"(String testNumber, long requiredAvailableTokens, Bucket bucket) {
        setup:
            BucketState state = bucket.createSnapshot()
        when:
            long availableTokens = state.getAvailableTokens(bucket.configuration.bandwidths)
        then:
            availableTokens == requiredAvailableTokens
        where:
            [testNumber, requiredAvailableTokens, bucket] << [
                [
                        "#1",
                        10,
                        Bucket4j.builder()
                            .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ], [
                        "#2",
                        0,
                        Bucket4j.builder()
                            .addLimit(0, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ], [
                        "#3",
                        5,
                        Bucket4j.builder()
                            .addLimit(5, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ], [
                        "#4",
                        2,
                        Bucket4j.builder()
                            .addLimit(5, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .addLimit(Bandwidth.simple(2, Duration.ofNanos(100)))
                            .build()
                ], [
                        "#5",
                        10,
                        Bucket4j.builder()
                            .addLimit(Bandwidth.classic(10, Refill.smooth(1, Duration.ofSeconds(1))))
                            .build()
                ]
            ]
    }

    @Unroll
    def "addTokens specification #testNumber"(String testNumber, long tokensToAdd, long requiredAvailableTokens, Bucket bucket) {
        setup:
            BucketState state = bucket.createSnapshot()
        when:
            state.addTokens(bucket.configuration.bandwidths, tokensToAdd)
            long availableTokens = state.getAvailableTokens(bucket.configuration.bandwidths)
        then:
            availableTokens == requiredAvailableTokens
        where:
            [testNumber, tokensToAdd, requiredAvailableTokens, bucket] << [
                [
                        "#1",
                        10,
                        10,
                        Bucket4j.builder()
                                .addLimit(0, Bandwidth.simple(10, Duration.ofNanos(100)))
                                .build()
                ], [
                        "#2",
                        1,
                        10,
                        Bucket4j.builder()
                                .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)))
                                .build()
                ], [
                        "#3",
                        6,
                        10,
                        Bucket4j.builder()
                                .addLimit(5, Bandwidth.simple(10, Duration.ofNanos(100)))
                                .build()
                ], [
                        "#4",
                        3,
                        2,
                        Bucket4j.builder()
                                .addLimit(5, Bandwidth.simple(10, Duration.ofNanos(100)))
                                .addLimit(Bandwidth.simple(2, Duration.ofNanos(100)))
                                .build()
                ], [
                        "#5",
                        4,
                        5,
                        Bucket4j.builder()
                                .addLimit(1, Bandwidth.classic(10, Refill.smooth(1, Duration.ofSeconds(1))))
                                .build()
                ]
        ]
    }

    @Unroll
    def "delayAfterWillBePossibleToConsume specification #testNumber"(String testNumber, long toConsume, long requiredTime, Bucket bucket) {
        def configuration = bucket.configuration
        setup:
            BucketState state = bucket.createSnapshot()
        when:
            long actualTime = state.delayNanosAfterWillBePossibleToConsume(configuration.bandwidths, toConsume)
        then:
            actualTime == requiredTime
        where:
            [testNumber, toConsume, requiredTime, bucket] << [
                [
                        "#1",
                        10,
                        100,
                        Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(0, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ], [
                        "#2",
                        10,
                        100,
                        Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(0, Bandwidth.classic(10, Refill.smooth(10, Duration.ofNanos(100))))
                            .build()
                ], [
                        "#3",
                        10,
                        500,
                        Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(0, Bandwidth.classic(10, Refill.smooth(2, Duration.ofNanos(100))))
                            .build()
                ], [
                        "#4",
                        7,
                        30,
                        Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(4, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ], [
                        "#5",
                        11,
                        70,
                        Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(4, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ], [
                        "#6",
                        3,
                        20,
                        Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .addLimit(2, Bandwidth.simple(5, Duration.ofNanos(10)))
                            .build()
                ], [
                        "#7",
                        3,
                        20,
                        Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(2, Bandwidth.simple(5, Duration.ofNanos(10)))
                            .addLimit(1, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ], [
                        "#8",
                        3,
                        0,
                        Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(5, Bandwidth.simple(5, Duration.ofNanos(10)))
                            .addLimit(3, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ]
            ]
    }

    @Unroll
    def "Specification for refill simple bandwidth #n"(int n, long initialTokens, long capacity, long period,
                                                       long initTime, long timeOnRefill, long tokensAfterRefill, long roundingError) {
        setup:
            TimeMeterMock mockTimer = new TimeMeterMock(initTime)
            Bucket bucket = Bucket4j.builder()
                    .addLimit(initialTokens, Bandwidth.simple(capacity, Duration.ofNanos(period)))
                    .withCustomTimePrecision(mockTimer)
                    .build()
            BucketState state = bucket.createSnapshot()
            BucketConfiguration configuration = bucket.getConfiguration()
        when:
            mockTimer.setCurrentTimeNanos(timeOnRefill)
            state.refillAllBandwidth(configuration.bandwidths, timeOnRefill)
        then:
            state.getCurrentSize(0) == tokensAfterRefill
            state.getRoundingError(0) == roundingError
        where:
        n  | initialTokens |    capacity    | period | initTime | timeOnRefill | tokensAfterRefill | roundingError
        1  |        0      |      1000      | 1000   | 10000    |     10040    |       40          |      0
        2  |       50      |      1000      | 1000   | 10000    |     10001    |       51          |      0
        3  |       55      |      1000      | 1000   | 10000    |      9999    |       55          |      0
        4  |      200      |      1000      | 1000   | 10000    |     20000    |     1000          |      0
        5  |        0      |       100      | 1000   | 10000    |     10003    |        0          |      300
        6  |       90      |       100      | 1000   | 10000    |     10017    |       91          |      700
        7  |        0      |       100      | 1000   | 10000    |     28888    |      100          |      0
    }

    @Unroll
    def "Specification for refill classic bandwidth #n"(int n, long initialTokens, long capacity, long refillTokens, long refillPeriod,
                                                       long initTime, long timeOnRefill, long tokensAfterRefill, long roundingError) {
        setup:
            TimeMeterMock mockTimer = new TimeMeterMock(initTime)
            def refill = Refill.smooth(refillTokens, Duration.ofNanos(refillPeriod))
            Bucket bucket = Bucket4j.builder()
                    .addLimit(initialTokens, Bandwidth.classic(capacity, refill))
                    .withCustomTimePrecision(mockTimer)
                    .build()
            BucketState state = bucket.createSnapshot()
            BucketConfiguration configuration = bucket.getConfiguration()
        when:
            mockTimer.setCurrentTimeNanos(timeOnRefill)
            state.refillAllBandwidth(configuration.bandwidths, timeOnRefill)
        then:
            state.getCurrentSize(0) == tokensAfterRefill
            state.getRoundingError(0) == roundingError
        where:
        n  | initialTokens |    capacity    | refillTokens | refillPeriod | initTime | timeOnRefill | tokensAfterRefill | roundingError
        1  |        0      |      1000      |       1      |          1   | 10000    |     10040    |       40          |      0
        2  |       50      |      1000      |      10      |         10   | 10000    |     10001    |       51          |      0
        3  |       55      |      1000      |       1      |          1   | 10000    |     10000    |       55          |      0
        4  |      200      |      1000      |      10      |         10   | 10000    |     20000    |     1000          |      0
        5  |        0      |       100      |       1      |         10   | 10000    |     10003    |        0          |      3
        6  |       90      |       100      |       1      |         10   | 10000    |     10017    |       91          |      7
        7  |        0      |       100      |       1      |         10   | 10000    |     28888    |      100          |      0
    }

    @Unroll
    def "Specification for consume #n"(int n, long initialTokens, long period,
                                       long capacity, long toConsume, long requiredSize
                                       ) {
        setup:
            Bucket bucket = Bucket4j.builder()
                .addLimit(initialTokens, Bandwidth.simple(capacity, Duration.ofNanos(period)))
                .build()
            BucketState state = bucket.createSnapshot()
            BucketConfiguration configuration = bucket.getConfiguration()
        when:
            state.consume(configuration.bandwidths, toConsume)
        then:
            state.getCurrentSize(0) == requiredSize
        where:
        n  |  initialTokens  | period | capacity | toConsume | requiredSize
        1  |        0        | 1000   |   1000   |    10     |   -10
        2  |       50        | 1000   |   1000   |     2     |    48
        3  |       55        | 1000   |   1000   |   1600    |   -1545
    }

}
