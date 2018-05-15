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

import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification

import java.time.Duration


class BucketRoundingRulesSpecification extends Specification {

    def "rest of division should not be missed on next consumption"() {
        setup:
            TimeMeterMock meter = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                                .withCustomTimePrecision(meter)
                                .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
                                .build()
        when:
            meter.setCurrentTimeNanos(97)
        then:
            bucket.tryConsume(9)
            !bucket.tryConsume(1)
        when:
            meter.addTime(3)
        then:
            bucket.tryConsume(1)
            !bucket.tryConsume(1)
    }

    def "rest of division should cleared when addTokens increases bucket to maximum"() {
        setup:
            TimeMeterMock meter = new TimeMeterMock(0)
            Bucket bucket = Bucket4j.builder()
                    .withCustomTimePrecision(meter)
                    .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
                    .build()
        when:
            meter.setCurrentTimeNanos(97)
        then:
            bucket.tryConsume(9)
            !bucket.tryConsume(1)
        when:
            bucket.addTokens(10)
        then:
            bucket.tryConsume(10)
        when:
            meter.addTime(3)
        then:
            !bucket.tryConsume(1)
    }

}
