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

import com.github.bucket4j.mock.AdjusterMock
import com.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

class BandwidthSpecification extends Specification {

    def "Specification for initialization"(long period, long capacity, long initialCapacity, long currentTime) {
        setup:
            def meter = new TimeMeterMock(currentTime);
            def builder = Buckets.withCustomTimePrecision(meter)
                .withLimitedBandwidth(capacity, TimeUnit.MINUTES, period, initialCapacity)
            def bucket = builder.build()
            def bandwidth = bucket.getConfiguration().getBandwidth(0)
        when:
            def state = bucket.createSnapshot()
        then:
            bandwidth.getCurrentSize(state) == initialCapacity
        where:
            period | capacity | initialCapacity | currentTime
              10   |   100    |      50         |    10000
              10   |    70    |      80         |    10000
    }

    def "Specification for timeRequiredToRefill"(long period, long capacity, long initialCapacity, long currentTime,
                 long tokensToConsume, long requiredTime) {
        setup:
            def meter = new TimeMeterMock(currentTime);
            def builder = Buckets.withCustomTimePrecision(meter)
                    .withLimitedBandwidth(capacity, TimeUnit.NANOSECONDS, period, initialCapacity)
            def bucket = builder.build()
            def bandwidth = bucket.getConfiguration().getBandwidth(0)
            def state = bucket.createSnapshot()
        expect:
            bandwidth.delayAfterWillBePossibleToConsume(state, currentTime, tokensToConsume) == requiredTime
        where:
            period | capacity | initialCapacity | currentTime | tokensToConsume | requiredTime
              10   |   100    |       100       |    10000    |      101        |  Long.MAX_VALUE
              10   |   100    |       100       |    10000    |      100        |        0
              10   |   100    |       100       |    10000    |       99        |        0
              10   |   100    |        80       |    10000    |      100        |        2
              10   |   100    |        80       |    10000    |       90        |        1
    }

    @Unroll
    def "Specification for refill #n"(int n, long initialCapacity, long period, long initTime,
               long maxCapacityBefore, long maxCapacityAfter, long timeRefill1, long requiredSize1,
               long timeRefill2, long requiredSize2, long timeRefill3, long requiredSize3) {
        setup:
            def meter = new TimeMeterMock(initTime);
            def adjuster = new AdjusterMock(maxCapacityBefore)
            def bucket = Buckets.withCustomTimePrecision(meter)
                    .withLimitedBandwidth(adjuster, TimeUnit.NANOSECONDS, period, initialCapacity)
                    .build()
            def bandwidth = bucket.getConfiguration().getBandwidth(0)
            def bandwidths = bucket.getConfiguration().getBandwidths()
            def state = bucket.createSnapshot()
        when:
            adjuster.setCapacity(maxCapacityAfter)
            meter.setCurrentTime(timeRefill1)
            state.refill(bandwidths, timeRefill1)
        then:
            bandwidth.getCurrentSize(state) == requiredSize1
            bandwidth.getMaxCapacity(timeRefill1) == maxCapacityAfter
        when:
            adjuster.setCapacity(maxCapacityAfter)
            meter.setCurrentTime(timeRefill2)
            state.refill(bandwidths, timeRefill2)
        then:
            bandwidth.getCurrentSize(state) == requiredSize2
            bandwidth.getMaxCapacity(timeRefill1) == maxCapacityAfter
        when:
            adjuster.setCapacity(maxCapacityAfter)
            meter.setCurrentTime(timeRefill3)
            state.refill(bandwidths, timeRefill3)
        then:
            bandwidth.getCurrentSize(state) == requiredSize3
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
                                      long    capacity , long timeRefill1, long consume1, long requiredSize1,
                                      long timeRefill2, long consume2, long requiredSize2) {
        setup:
            def meter = new TimeMeterMock(initTime);
            def adjuster = new AdjusterMock(   capacity )
            def bucket = Buckets.withCustomTimePrecision(meter)
                    .withLimitedBandwidth(adjuster, TimeUnit.NANOSECONDS, period, initialCapacity)
                    .build()
            def bandwidth = bucket.getConfiguration().getBandwidth(0)
            def bandwidths = bucket.getConfiguration().getBandwidths()
            def state = bucket.createSnapshot()
        when:
            meter.setCurrentTime(timeRefill1)
            state.refill(bandwidths, timeRefill1)
            state.consume(bandwidths, consume1)
        then:
            bandwidth.getCurrentSize(state) == requiredSize1
        when:
            meter.setCurrentTime(timeRefill2)
            state.refill(bandwidths, timeRefill2)
            state.consume(bandwidths, consume2)
        then:
            bandwidth.getCurrentSize(state) == requiredSize2
        where:
            n  | initialCapacity | period | initTime |    capacity | timeRefill1 |  consume1 | requiredSize1 | timeRefill2 | consume2 | requiredSize2
            1  |        0        | 1000   | 10000    |      1000   |     10040   |     10    |     30        |    10050    |    20    |       20
            2  |       50        | 1000   | 10050    |      1000   |     10051   |      2    |     49        |    10055    |     7    |       46
            3  |       55        | 1000   | 10055    |      1000   |     10500   |     600   |      0        |    10504    |     3    |       1
    }

}
