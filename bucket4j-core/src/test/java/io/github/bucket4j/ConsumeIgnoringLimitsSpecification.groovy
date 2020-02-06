/*
 *
 * Copyright 2015-2020 Vladimir Bukhtoyarov
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

package io.github.bucket4j;

import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import spock.lang.Specification;
import spock.lang.Unroll;

import java.time.Duration
import java.util.concurrent.ExecutionException

import static org.junit.Assert.fail;

class ConsumeIgnoringLimitsSpecification extends Specification {

    @Unroll
    def "#n case when limits are not overflown"(int n, long tokensToConsume, long nanosIncrement, long remainedTokens, AbstractBucketBuilder builder) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(builder, timeMeter)
                timeMeter.addTime(nanosIncrement)
                if (sync) {
                    assert bucket.consumeIgnoringRateLimits(tokensToConsume) == 0
                } else {
                    bucket.asAsync().consumeIgnoringRateLimits(tokensToConsume).get() == 0
                }
                assert bucket.createSnapshot().getAvailableTokens(bucket.configuration.bandwidths) == remainedTokens
            }
        }
        where:
        n | tokensToConsume | nanosIncrement | remainedTokens | builder
        1 |     49          |     50         |        1       | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
        2 |     50          |     50         |        0       | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
        3 |     51          |     120        |        49      | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
        4 |     100         |     101        |        0       | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
    }

    @Unroll
    def "#n case when limits are overflown"(int n, long tokensToConsume, long nanosIncrement, long remainedTokens, long overflowNanos,  AbstractBucketBuilder builder) {
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(builder, timeMeter)
                timeMeter.addTime(nanosIncrement)
                if (sync) {
                    assert bucket.consumeIgnoringRateLimits(tokensToConsume) == overflowNanos
                } else {
                    bucket.asAsync().consumeIgnoringRateLimits(tokensToConsume).get() == overflowNanos
                }
                assert bucket.createSnapshot().getAvailableTokens(bucket.configuration.bandwidths) == remainedTokens
            }
        }
        where:
        n | tokensToConsume | nanosIncrement | remainedTokens | overflowNanos   | builder
        1 |     52          |      50        |       -2       |      2          | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
        2 |     50          |      0         |      -50       |      50         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
        3 |    151          |      120       |      -51       |      51         | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
        4 |    400          |      201       |      -300      |      300        | Bucket4j.builder().addLimit(Bandwidth.simple(100, Duration.ofNanos(100)).withInitialTokens(0))
    }

    def "Reservation overflow case"() {
        setup:
            AbstractBucketBuilder builder = Bucket4j.builder()
                .addLimit(Bandwidth.simple(1, Duration.ofMinutes(1)).withInitialTokens(0))
            long veryBigAmountOfTokensWhichCannotBeReserved = Long.MAX_VALUE / 2;
        expect:
        for (BucketType type : BucketType.values()) {
            for (boolean sync : [true, false]) {
                TimeMeterMock timeMeter = new TimeMeterMock(0)
                Bucket bucket = type.createBucket(builder, timeMeter)
                if (sync) {
                    try {
                        bucket.consumeIgnoringRateLimits(veryBigAmountOfTokensWhichCannotBeReserved)
                        fail()
                    } catch (IllegalArgumentException e) {
                        assert e.message == BucketExceptions.reservationOverflow().message
                    }
                } else {
                    try {
                        bucket.asAsync().consumeIgnoringRateLimits(veryBigAmountOfTokensWhichCannotBeReserved).get()
                        fail()
                    } catch(ExecutionException e) {
                        assert e.getCause().message == BucketExceptions.reservationOverflow().message
                    }
                }
            }
        }
    }

}
