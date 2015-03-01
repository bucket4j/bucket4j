/*
 * Copyright 2012-2014 Brandon Beck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vbukhtoyarov.concurrency.tokenbucket.refill

import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

public class FixedIntervalRefillStrategyTest extends Specification {

    @Unroll
    def "strategy which adding #bucketSize tokens each #period #unit should require #requiredNanos nanos to generate #tokens tokens"(long bucketSize, long period, TimeUnit unit, long tokens, long requiredNanos) {
        FixedIntervalRefillStrategy strategy = new FixedIntervalRefillStrategy(bucketSize, period, unit);

        expect:
        strategy.nanosRequiredToRefill(tokens) == requiredNanos

        where:
        bucketSize  | period | unit                 | tokens | requiredNanos
        1000        | 100    | TimeUnit.NANOSECONDS | 1000   | 100
        1000        | 100    | TimeUnit.NANOSECONDS | 500    | 50
        1000        | 100    | TimeUnit.NANOSECONDS | 100    | 10
    }

    @Unroll
    def "strategy which adding #bucketSize tokens each #period #unit, should return #requiredRefill refill, when previous refill is #previousRefillNanos and current time nanos is #currentNanos"(long bucketSize, long period, TimeUnit unit, long previousRefillNanos, long currentNanos, long requiredRefill) {
        FixedIntervalRefillStrategy strategy = new FixedIntervalRefillStrategy(bucketSize, period, unit);

        expect:
        strategy.refill(previousRefillNanos, currentNanos) == requiredRefill;

        where:
        bucketSize | period | unit                 | previousRefillNanos | currentNanos | requiredRefill
        666        | 100    | TimeUnit.NANOSECONDS | 1000                | 1100         | 666
        666        | 100    | TimeUnit.NANOSECONDS | 1000                | 1101         | 666
        666        | 100    | TimeUnit.NANOSECONDS | 1000                | 1099         | 0
        666        | 1      | TimeUnit.SECONDS     | 1000                | 1101         | 0
    }

}
