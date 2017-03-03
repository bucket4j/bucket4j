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
import spock.lang.Specification
import spock.lang.Unroll
import java.time.Duration

import static com.github.bucket4j.BucketExceptions.*

public class DetectionOfIllegalApiUsageSpecification extends Specification {

    private static final long VALID_PERIOD = 10;
    private static final long VALID_CAPACITY = 1000;

    @Unroll
    def "Should detect that capacity #capacity is wrong"(long capacity) {
        when:
             Bucket4j.builder().withLimitedBandwidth(capacity, Duration.ofMinutes(VALID_PERIOD))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveCapacity(capacity).message
        where:
          capacity << [-10, -5, 0]
    }

    @Unroll
    def "Should detect that initial capacity #initialCapacity is wrong"(long initialCapacity) {
        when:
            Bucket4j.builder().withLimitedBandwidth(VALID_CAPACITY, initialCapacity, Duration.ofMinutes(VALID_PERIOD))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveInitialCapacity(initialCapacity).message
        where:
            initialCapacity << [-10, -1]
    }

    @Unroll
    def "Should check that #period is invalid period of bandwidth"(long period) {
        when:
            Bucket4j.builder().withLimitedBandwidth(VALID_CAPACITY, Duration.ofMinutes(period))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositivePeriod(Duration.ofMinutes(period).toNanos()).message
        where:
            period << [-10, -1, 0]
    }

    def "Should check that bandwidth adjuster is not null"() {
        when:
            Bucket4j.builder().withLimitedBandwidth(null, VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nullBandwidthAdjuster().message
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

        when:
           builder.withGuaranteedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD)).build()
        then:
            ex = thrown()
            ex.message == restrictionsNotSpecified().message
    }

    def "Should check that guaranteed capacity could not be configured twice"() {
        setup:
            def builder = Bucket4j.builder()
                .withLimitedBandwidth(VALID_CAPACITY * 2, Duration.ofMinutes(VALID_PERIOD))
                .withGuaranteedBandwidth(VALID_CAPACITY + 1, Duration.ofMinutes(VALID_PERIOD))
                .withGuaranteedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
        when:
            builder.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == onlyOneGuarantedBandwidthSupported().message
    }

    def "Should check that guaranteed bandwidth has lesser rate than limited bandwidth"() {
        setup:
            def builder1 = Bucket4j.builder()
                .withLimitedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
                .withGuaranteedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
            def builder2 = Bucket4j.builder()
                .withGuaranteedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
                .withLimitedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
            def builder3 = Bucket4j.builder()
                .withLimitedBandwidth(new AdjusterMock(VALID_CAPACITY), 0, Duration.ofMinutes(VALID_PERIOD))
                .withGuaranteedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
            def builder4 = Bucket4j.builder()
                .withLimitedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
                .withGuaranteedBandwidth(new AdjusterMock(VALID_CAPACITY), 0, Duration.ofMinutes(VALID_PERIOD))
        when:
            builder1.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == guarantedHasGreaterRateThanLimited(builder1.getBandwidthDefinition(1), builder1.getBandwidthDefinition(0)).message
        when:
            builder2.build()
        then:
            ex = thrown()
            ex.message == guarantedHasGreaterRateThanLimited(builder2.getBandwidthDefinition(0), builder2.getBandwidthDefinition(1)).message
        when:
            builder3.build()
        then:
            notThrown()
        when:
            builder4.build()
        then:
            notThrown()
    }

    @Unroll
    def "Should check for overlaps, test #number"(int number, long firstCapacity, long firstPeriod, long secondCapacity, long secondPeriod) {
        setup:
            def builderWithStaticCapacity = Bucket4j.builder()
                .withLimitedBandwidth(firstCapacity, Duration.ofMinutes(firstPeriod))
                .withLimitedBandwidth(secondCapacity, Duration.ofMinutes(secondPeriod))
            def builderWithDynamicCapacity1 = Bucket4j.builder()
                .withLimitedBandwidth(new AdjusterMock(firstCapacity), 0, Duration.ofMinutes(firstPeriod))
                .withLimitedBandwidth(secondCapacity, Duration.ofMinutes(secondPeriod))
            def builderWithDynamicCapacity2 = Bucket4j.builder()
                .withLimitedBandwidth(firstCapacity, Duration.ofMinutes(firstPeriod))
                .withLimitedBandwidth(new AdjusterMock(secondCapacity), 0, Duration.ofMinutes(secondPeriod))
        when:
            builderWithStaticCapacity.build()
        then:
            IllegalArgumentException ex = thrown()
            ex.message == hasOverlaps(builderWithStaticCapacity.getBandwidthDefinition(0), builderWithStaticCapacity.getBandwidthDefinition(1)).message
        when:
            builderWithDynamicCapacity1.build()
        then:
            notThrown()
        when:
            builderWithDynamicCapacity2.build()
        then:
            notThrown()
        where:
            number |  firstCapacity | firstPeriod | secondCapacity | secondPeriod
               1   |     999        |     10      |      999       |      10
               2   |     999        |     10      |      1000      |      10
               3   |     999        |     10      |      998       |      10
               4   |     999        |     10      |      999       |      11
               5   |     999        |     10      |      999       |      9
               6   |     999        |     10      |      1000      |      8
               7   |     999        |     10      |      998       |      11
    }

    def "Should check that tokens to consume should be positive"() {
        setup:
            def bucket = Bucket4j.builder()
                    .withLimitedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
                    .build()
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
            bucket.consumeAsMuchAsPossible(0)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.consumeAsMuchAsPossible(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message

        when:
            bucket.tryConsume(0, VALID_PERIOD)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(0).message

        when:
            bucket.tryConsume(-1, VALID_PERIOD)
        then:
            ex = thrown()
            ex.message == nonPositiveTokensToConsume(-1).message
    }

    def "Should check that time units to wait should be positive"() {
        setup:
            def bucket = Bucket4j.builder()
                    .withLimitedBandwidth(VALID_CAPACITY, Duration.ofMinutes(VALID_PERIOD))
                    .build()
        when:
            bucket.tryConsumeSingleToken(0)
        then:
            IllegalArgumentException ex = thrown()
            ex.message == nonPositiveNanosToWait(0).message

        when:
            bucket.tryConsumeSingleToken(-1)
        then:
            ex = thrown()
            ex.message == nonPositiveNanosToWait(-1).message
    }

}
