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
import java.time.Instant


class IntervallyAlignedRefillSpecification extends Specification {


    @Unroll
    def "#n Initial token calculation spec"(int n, long currentTimeMillis, long firstRefillTimeMillis, long capacity,
            long refillTokens, long refillPeriodMillis, long requiredInitialTokens) {
        setup:
            Instant firstRefillTime = new Date(firstRefillTimeMillis).toInstant()
            Duration refillPeriod = Duration.ofMillis(refillPeriodMillis)
            Refill refill = Refill.intervallyAligned(refillTokens, refillPeriod, firstRefillTime, true)
            Bandwidth bandwidth = Bandwidth.classic(capacity, refill)
            TimeMeterMock mockTimer = new TimeMeterMock(currentTimeMillis * 1_000_000)
            Bucket bucket = Bucket4j.builder()
                    .withCustomTimePrecision(mockTimer)
                    .addLimit(bandwidth)
                    .build()

        expect:
            bucket.getAvailableTokens() == requiredInitialTokens

        where:
            n | currentTimeMillis | firstRefillTimeMillis | capacity  |  refillTokens | refillPeriodMillis | requiredInitialTokens
            0 |      60_000       |        60_000         |    400    |       400     |       60_000       |          400
            1 |      20_000       |        60_000         |    400    |       400     |       60_000       |          266
            2 |      20_000       |        60_000         |    400    |       300     |       60_000       |          300
            3 |      20_000       |        60_000         |    400    |       200     |       60_000       |          333
            4 |      20_000       |        60_000         |    400    |       100     |       60_000       |          366
            5 |      60_000       |        60_000         |    400    |       100     |       60_000       |          400
            6 |      60_001       |        60_000         |    400    |       100     |       60_000       |          400
            7 |      20_000       |       180_000         |    400    |       100     |       60_000       |          400
            8 |      20_000       |        60_000         |    100    |       400     |       60_000       |          100
            9 |      59_000       |        60_000         |    100    |       400     |       60_000       |          6
    }

}
