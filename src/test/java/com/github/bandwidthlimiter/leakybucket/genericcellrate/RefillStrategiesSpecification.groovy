/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.bandwidthlimiter.leakybucket.genericcellrate

import com.github.bandwidthlimiter.leakybucket.Bandwidth
import com.github.bandwidthlimiter.leakybucket.genericcellrate.RefillStrategy
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static com.github.bandwidthlimiter.leakybucket.genericcellrate.RefillStrategy.*
import static java.util.concurrent.TimeUnit.*

public class RefillStrategiesSpecification extends Specification {

    @Unroll
    def "Refill specification, test number #number"(int number, long capacity, long period, TimeUnit unit, long previousRefillNanos,
            currentNanos, long requiredRefill, RefillStrategy strategy) {
        setup:
            def bandwidth = new Bandwidth(capacity, period, unit)
        when:
            def refill = strategy.refill(bandwidth, previousRefillNanos, currentNanos)
        then:
            requiredRefill == refill
        where:
        number | capacity   | period   |    unit     | previousRefillNanos | currentNanos | requiredRefill| strategy
             1 |    666     |   100    | NANOSECONDS | 1000                | 1100         | 666           | BURST
             2 |    666     |   100    | NANOSECONDS | 1000                | 1101         | 666           | BURST
             3 |    666     |   100    | NANOSECONDS | 1000                | 1099         | 0             | BURST
             4 |    666     |    1     | SECONDS     | 1000                | 1101         | 0             | BURST
             5 |    666     |   100    | NANOSECONDS | 1000                | 1100         | 666           | MONOTONE
             6 |    666     |   100    | NANOSECONDS | 1000                | 2100         | 666           | MONOTONE
             7 |    100     |   100    | NANOSECONDS | 1000                | 1099         | 99            | MONOTONE
             8 | 1000000000 |    1     | SECONDS     | 0                   | 999999999    | 999999999     | MONOTONE
             9 | 1000000000 |    1     | SECONDS     | 0                   |1000000001    |1000000000     | MONOTONE
    }

    @Unroll
    def "'Nanos required to Refill' specification, test number #number"(int number, long capacity, long period,
            TimeUnit unit, long tokens, long nanosRequired, RefillStrategy strategy) {
        setup:
            def bandwidth = new Bandwidth(capacity, period, unit)
        when:
            def nanosActual = strategy.nanosRequiredToRefill(bandwidth, tokens)
        then:
            nanosActual == nanosRequired
        where:
        number | capacity    | period | unit        | tokens | nanosRequired | strategy
             1 | 1000        | 100    | NANOSECONDS | 1000   | 100           | BURST
             2 | 1000        | 100    | NANOSECONDS | 500    | 50            | BURST
             3 | 1000        | 100    | NANOSECONDS | 100    | 10            | BURST
             4 | 1000        | 100    | NANOSECONDS | 5000   | 500           | BURST
             5 | 1000        | 100    | NANOSECONDS | 1000   | 100           | MONOTONE
             6 | 1000        | 100    | NANOSECONDS | 500    | 50            | MONOTONE
             7 | 1000        | 100    | NANOSECONDS | 100    | 10            | MONOTONE
             8 | 1000        | 100    | NANOSECONDS | 5000   | 500           | MONOTONE
    }

}
