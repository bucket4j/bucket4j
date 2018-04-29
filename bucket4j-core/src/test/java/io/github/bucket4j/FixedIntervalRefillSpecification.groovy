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

import java.time.Duration

class FixedIntervalRefillSpecification extends Specification {

    def "Basic test of fixed interval refill"() {
        setup:
           Bandwidth bandwidth = Bandwidth.simple(9, Duration.ofNanos(10))
                .withInitialTokens(0)
                .withFixedRefillInterval(Duration.ofNanos(5))
            TimeMeterMock mockTimer = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                .withCustomTimePrecision(mockTimer)
                .addLimit(bandwidth)
                .build()

        expect:
            bucket.getAvailableTokens() == 0

        when:
            mockTimer.addTime(4)
        then:
            bucket.getAvailableTokens() == 0

        when:
            mockTimer.addTime(1)
        then:
            bucket.getAvailableTokens() == 4

        when:
            mockTimer.addTime(4)
        then:
            bucket.getAvailableTokens() == 4

        when:
            mockTimer.addTime(1)
        then:
            bucket.getAvailableTokens() == 9
    }

    def "Complex test of fixed interval refill"() {
        setup:
            Bandwidth bandwidth1 = Bandwidth.simple(9, Duration.ofNanos(10))
                    .withInitialTokens(0)
                    .withFixedRefillInterval(Duration.ofNanos(5))
            Bandwidth bandwidth2 = Bandwidth.simple(12, Duration.ofNanos(15))
                .withInitialTokens(0)
                .withFixedRefillInterval(Duration.ofNanos(3))
            TimeMeterMock mockTimer = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                    .withCustomTimePrecision(mockTimer)
                    .addLimit(bandwidth1)
                    .addLimit(bandwidth2)
                    .build()
        expect:
            bucket.getAvailableTokens() == 0

        when:
            mockTimer.addTime(4) // 4
        then:
            bucket.getAvailableTokens() == 0

        when:
            mockTimer.addTime(1) // 5
        then:
            bucket.getAvailableTokens() == 2

        when:
            mockTimer.addTime(4) // 9
        then:
            bucket.getAvailableTokens() == 4

        when:
            mockTimer.addTime(1) // 10
        then:
            bucket.getAvailableTokens() == 7

        when:
            mockTimer.addTime(2) // 12
        then:
            bucket.getAvailableTokens() == 9

    }

}
