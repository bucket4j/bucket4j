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

import spock.lang.Specification
import spock.lang.Unroll
import java.time.Duration

import static BucketExceptions.*

class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final Duration VALID_PERIOD = Duration.ofMinutes(10);
    private static final long VALID_CAPACITY = 1000;

    ConfigurationBuilder builder = Bucket4j.builder()

    @Unroll
    def "Should detect that capacity #capacity is wrong"(long capacity) {
        when:
            builder.addLimit(Bandwidth.classic(capacity, Refill.smooth(1, VALID_PERIOD)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveCapacity(capacity).message

        where:
          capacity << [-10, -5, 0]
    }

    @Unroll
    def "Should detect that initialTokens #initialTokens is wrong"(long initialTokens) {
        when:
            builder.addLimit(initialTokens, Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveInitialTokens(initialTokens).message
        where:
            initialTokens << [-10, -1]
    }

    @Unroll
    def "Should check that #period is invalid period of bandwidth"(long period) {
        when:
            builder.addLimit(Bandwidth.simple(VALID_CAPACITY, Duration.ofMinutes(period)))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriod(Duration.ofMinutes(period).toNanos()).message

        where:
            period << [-10, -1, 0]
    }

    def "Should check that refill is not null"() {
        when:
            builder.addLimit(Bandwidth.classic(42, null))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullBandwidthRefill().message
    }

    def "Should check than time meter is not null"() {
        when:
            Bucket4j.builder().withCustomTimePrecision(null)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullTimeMeter().message
    }

    def  "Should check that limited bandwidth list is not empty"() {
        setup:
            def builder = Bucket4j.builder()
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == restrictionsNotSpecified().message
    }

    def "Should check that tokens to consume should be positive"() {
        setup:
            def bucket = Bucket4j.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()
        when:
            bucket.consume(0)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.consume(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.tryConsume(0)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.tryConsume(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.tryConsumeAsMuchAsPossible(0)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.tryConsumeAsMuchAsPossible(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.consume(0L, VALID_PERIOD.toNanos())
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.consume(-1, VALID_PERIOD.toNanos())
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message
    }

    def "Should check that time units to wait should be positive"() {
        setup:
            def bucket = Bucket4j.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()
        when:
            bucket.consume(1, 0)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveNanosToWait(0).message

        when:
            bucket.consume(1, -1)
        then:
            ex = thrown()
            ex.message == nonPositiveNanosToWait(-1).message
    }

    @Unroll
    def "Should check that #tokens tokens is not positive to add"(long tokens) {
        setup:
            def bucket = Bucket4j.builder().addLimit(
                    Bandwidth.simple(VALID_CAPACITY, VALID_PERIOD)
            ).build()
        when:
            bucket.addTokens(tokens)
        then:
            thrown(IllegalArgumentException)
        where:
            tokens << [0, -1, -10]
    }

}
