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
import java.util.concurrent.TimeUnit


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


    def "complex spec for case when useAdaptiveInitialTokens=false"() {
        setup: """
                  Having the refill 200 tokens/1 minute, capacity is 400, 
                  20 seconds past from beginning of current minute, 
                  first refill planned to next minute
               """
            Instant firstRefillTime = new Date(TimeUnit.SECONDS.toMillis(120)).toInstant()
            Refill refill = Refill.intervallyAligned(200, Duration.ofMinutes(1), firstRefillTime, false)
            Bandwidth bandwidth = Bandwidth.classic(400, refill)
            TimeMeterMock mockTimer = new TimeMeterMock()
            mockTimer.setCurrentTimeSeconds(80)

            Bucket bucket = Bucket4j.builder()
                .withCustomTimePrecision(mockTimer)
                .addLimit(bandwidth)
                .build()

        expect: "initialTokens == capacity because useAdaptiveInitialTokens == false"
            bucket.getAvailableTokens() == 400

        when: "when all tokens consumed and 10 seconds elapsed"
            bucket.tryConsumeAsMuchAsPossible()
            mockTimer.addSeconds(10)

        then: "available tokens should be zero because refill is not greedy"
            bucket.getAvailableTokens() == 0
        and: "bucket should report that it is need to wait 30 seconds before first token will be available"
            bucket.tryConsumeAndReturnRemaining(1).nanosToWaitForRefill == TimeUnit.SECONDS.toNanos(30)

        when: "yet another 30 seconds elapsed"
            mockTimer.addSeconds(30)
        then: "200 tokens should be added to bucket"
            bucket.getAvailableTokens() == 200

        when: "yet another 45 seconds elapsed"
            mockTimer.addSeconds(45)
        then: "nothing should be added to bucket"
            bucket.getAvailableTokens() == 200

        when: "yet another 15 seconds elapsed"
            mockTimer.addSeconds(15)
        then: "200 tokens should be added to bucket"
            bucket.getAvailableTokens() == 400

        when: "yet another 60 seconds elapsed"
            mockTimer.addSeconds(60)
        then: "nothing should be added to bucket because max capacity already reached"
            bucket.getAvailableTokens() == 400

        when: "all tokens consumed"
            bucket.tryConsumeAsMuchAsPossible()
        then: "bucket should report that 3 minute required to wait in order to consume 401 tokens"
            bucket.tryConsumeAndReturnRemaining(401).nanosToWaitForRefill == TimeUnit.MINUTES.toNanos(3)
    }

    def "complex spec for case when useAdaptiveInitialTokens=true"() {
        setup: """
                  Having the refill 200 tokens/1 minute, capacity is 400, 
                  20 seconds past from beginning of current minute, 
                  first refill planned to next minute
               """
            Instant firstRefillTime = new Date(TimeUnit.SECONDS.toMillis(120)).toInstant()
            Refill refill = Refill.intervallyAligned(200, Duration.ofMinutes(1), firstRefillTime, true)
            Bandwidth bandwidth = Bandwidth.classic(400, refill)
            TimeMeterMock mockTimer = new TimeMeterMock()
            mockTimer.setCurrentTimeSeconds(80)

            Bucket bucket = Bucket4j.builder()
                    .withCustomTimePrecision(mockTimer)
                    .addLimit(bandwidth)
                    .build()

        expect: "initialTokens == capacity because useAdaptiveInitialTokens == false"
            bucket.getAvailableTokens() == 333

        when: "when all tokens consumed and 10 seconds elapsed"
            bucket.tryConsumeAsMuchAsPossible()
            mockTimer.addSeconds(10)

        then: "available tokens should be zero because refill is not greedy"
            bucket.getAvailableTokens() == 0
        and: "bucket should report that it is need to wait 30 seconds before first token will be available"
            bucket.tryConsumeAndReturnRemaining(1).nanosToWaitForRefill == TimeUnit.SECONDS.toNanos(30)

        when: "yet another 30 seconds elapsed"
            mockTimer.addSeconds(30)
        then: "200 tokens should be added to bucket"
            bucket.getAvailableTokens() == 200

        when: "yet another 45 seconds elapsed"
            mockTimer.addSeconds(45)
        then: "nothing should be added to bucket"
            bucket.getAvailableTokens() == 200

        when: "yet another 15 seconds elapsed"
            mockTimer.addSeconds(15)
        then: "200 tokens should be added to bucket"
            bucket.getAvailableTokens() == 400

        when: "yet another 60 seconds elapsed"
            mockTimer.addSeconds(60)
        then: "nothing should be added to bucket because max capacity already reached"
            bucket.getAvailableTokens() == 400

        when: "all tokens consumed"
            bucket.tryConsumeAsMuchAsPossible()
        then: "bucket should report that 3 minute required to wait in order to consume 401 tokens"
            bucket.tryConsumeAndReturnRemaining(401).nanosToWaitForRefill == TimeUnit.MINUTES.toNanos(3)
    }

}
