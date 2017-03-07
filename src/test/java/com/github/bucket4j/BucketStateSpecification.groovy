/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.bucket4j

import com.github.bucket4j.mock.Mock
import com.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

import static java.lang.Long.MAX_VALUE


class BucketStateSpecification extends Specification {

    @Unroll
    def "GetAvailableTokens specification #testNumber"(String testNumber, long requiredAvailableTokens, Bucket bucket) {
        setup:
            BucketState state = bucket.createSnapshot()
        when:
            long availableTokens = state.getAvailableTokens(bucket.configuration.limitedBandwidths, bucket.configuration.guaranteedBandwidth)
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
                    3,
                    Bucket4j.builder()
                            .addLimit(5, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .addLimit(Bandwidth.simple(2, Duration.ofNanos(10)))
                            .setGuarantee(Bandwidth.simple(3, Duration.ofNanos(1000)))
                            .build()
                ], [
                    "#6",
                    2,
                    Bucket4j.builder()
                            .addLimit(5, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .addLimit(Bandwidth.simple(2, Duration.ofNanos(10)))
                            .setGuarantee(Bandwidth.simple(1, Duration.ofNanos(1000)))
                            .build()
                ], [
                    "#7",
                    10,
                    Bucket4j.builder()
                            .addLimit(Bandwidth.classic(10, Refill.smooth(1, Duration.ofSeconds(1))))
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
            long actualTime = state.delayNanosAfterWillBePossibleToConsume(configuration.limitedBandwidths, configuration.guaranteedBandwidth, 0, toConsume)
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
                    MAX_VALUE,
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
                            .setGuarantee(Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ], [
                    "#9",
                    3,
                    0,
                    Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(5, Bandwidth.simple(5, Duration.ofNanos(10)))
                            .addLimit(3, Bandwidth.simple(10, Duration.ofNanos(100)))
                            .build()
                ], [
                    "#10",
                    11,
                    11,
                    Bucket4j.builder()
                            .withCustomTimePrecision(new TimeMeterMock(0))
                            .addLimit(0, Bandwidth.simple(1000, Duration.ofNanos(1000))) // 1.0
                            .setGuarantee(Bandwidth.simple(10, Duration.ofNanos(100)))   // 0.1
                            .build()
                ]
            ]
    }

    @Unroll
    def "Specification for refill #n"(int n, long initialCapacity, long period, long initTime,
                                      long maxCapacityBefore, long maxCapacityAfter, long timeRefill1, long requiredSize1,
                                      long timeRefill2, long requiredSize2, long timeRefill3, long requiredSize3) {
        setup:
        def adjuster = new Mock(maxCapacityBefore)
        def bandwidth = new Bandwidth(adjuster, initialCapacity, period, false);
        def state = bandwidth.createInitialState()
        when:
        adjuster.setCapacity(maxCapacityAfter)
        bandwidth.refill(state, initTime, timeRefill1)
        then:
        state.getCurrentSize() == requiredSize1
        bandwidth.getMaxCapacity(timeRefill1) == maxCapacityAfter
        when:
        adjuster.setCapacity(maxCapacityAfter)
        bandwidth.refill(state, timeRefill1, timeRefill2)
        then:
        state.getCurrentSize() == requiredSize2
        bandwidth.getMaxCapacity(timeRefill1) == maxCapacityAfter
        when:
        adjuster.setCapacity(maxCapacityAfter)
        bandwidth.refill(state, timeRefill2, timeRefill3)
        then:
        state.getCurrentSize() == requiredSize3
        bandwidth.getMaxCapacity(timeRefill1) == maxCapacityAfter
        where:
        n  | initialCapacity | period | initTime | maxCapacityBefore | maxCapacityAfter | timeRefill1 | requiredSize1 | timeRefill2 | requiredSize2 | timeRefill3 | requiredSize3
        1  |        0        | 1000   | 10000    |      1000         |      1000        | 10040       |       40      |    10050    |    50         |    10090    |      90
        2  |       50        | 1000   | 10050    |      1000         |      1000        | 10051       |       51      |    10055    |    55         |    10100    |     100
        3  |       55        | 1000   | 10055    |      1000         |      1000        | 10500       |      500      |    11001    |  1000         |    12000    |    1000
        4  |     1000        | 1000   | 10000    |      1000         |       900        | 10200       |      900      |    10250    |   900         |    10251    |     900
        5  |      200        | 1000   | 10000    |      1000         |      1000        | 30000       |     1000      |    30001    |  1000         |    40000    |    1000
        6  |        0        | 1000   | 10000    |       100         |       100        | 10005       |        0      |    10010    |     1         |    10019    |       1
        7  |        0        | 1000   | 10000    |       100         |       100        | 10005       |        0      |    10009    |     0         |    10029    |       2
        8  |        0        | 1000   | 10000    |       100         |       100        | 10004       |        0      |    10009    |     0         |    10010    |       1
    }

    @Unroll
    def "Specification for consume #n"(int n, long initialCapacity, long period, long initTime,
                                       long capacity , long timeRefill1, long consume1, long requiredSize1,
                                       long timeRefill2, long consume2, long requiredSize2) {
        setup:
        Bandwidth bandwidth = bandwidth(capacity, initialCapacity, period)
        def state = bandwidth.createInitialState()
        when:
        bandwidth.refill(state, initTime, timeRefill1)
        bandwidth.consume(state, consume1)
        then:
        state.getCurrentSize() == requiredSize1
        when:
        bandwidth.refill(state, timeRefill1, timeRefill2)
        bandwidth.consume(state, consume2)
        then:
        state.getCurrentSize() == requiredSize2
        where:
        n  | initialCapacity | period | initTime |    capacity | timeRefill1 |  consume1 | requiredSize1 | timeRefill2 | consume2 | requiredSize2
        1  |        0        | 1000   | 10000    |      1000   |     10040   |     10    |     30        |    10050    |    20    |       20
        2  |       50        | 1000   | 10050    |      1000   |     10051   |      2    |     49        |    10055    |     7    |       46
        3  |       55        | 1000   | 10055    |      1000   |     10500   |     600   |      0        |    10504    |     3    |       1
    }

    private Bandwidth bandwidth(long capacity, long initialCapacity, long period) {
        return new Bandwidth(new Capacity.ImmutableCapacity(capacity), initialCapacity, period, false);
    }

}
