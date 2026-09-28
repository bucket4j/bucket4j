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

package io.github.bucket4j.core_algorithms;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.mock.BucketType;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bucket Rounding Rules Specification")
class BucketRoundingRulesTest {

    private record BucketTypeCase(BucketType bucketType) {

        @Override
        public String toString() {
            return "bucketType=" + bucketType;
        }
    }

    static Stream<BucketTypeCase> bucketTypes() {
        return Stream.of(BucketType.values()).map(BucketTypeCase::new);
    }

    @Test
    void restOfDivisionShouldNotBeMissedOnNextConsumption() throws Exception {
        TimeMeterMock meter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .withCustomTimePrecision(meter)
            .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
            .build();

        meter.setCurrentTimeNanos(97);

        assertThat(bucket.tryConsume(9)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();

        meter.addTime(3);

        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void restOfDivisionShouldClearedWhenAddTokensIncreasesBucketToMaximum() throws Exception {
        TimeMeterMock meter = new TimeMeterMock(0);
        Bucket bucket = Bucket.builder()
            .withCustomTimePrecision(meter)
            .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(0))
            .build();

        meter.setCurrentTimeNanos(97);

        assertThat(bucket.tryConsume(9)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();

        bucket.addTokens(10);

        assertThat(bucket.tryConsume(10)).isTrue();

        meter.addTime(3);

        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("bucketTypes")
    void partiallyRefilledTokenShouldNotBeMissedWhenCalculatingTimeForRefill(BucketTypeCase testCase) throws Exception {
        TimeMeterMock timeMeter = new TimeMeterMock(0);
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(4, Refill.greedy(1, Duration.ofSeconds(1))).withInitialTokens(1))
            .build();
        Bucket bucket = testCase.bucketType().createBucket(configuration, timeMeter);

        assertThat(bucket.tryConsumeAsMuchAsPossible()).isEqualTo(1);

        timeMeter.addTime(200_000_000);

        EstimationProbe estimationProbe1 = bucket.estimateAbilityToConsume(1);
        assertThat(estimationProbe1.canBeConsumed()).isFalse();
        assertThat(estimationProbe1.getNanosToWaitForRefill()).isEqualTo(800_000_000L);
        ConsumptionProbe consumptionProbe1 = bucket.tryConsumeAndReturnRemaining(1);
        assertThat(consumptionProbe1.isConsumed()).isFalse();
        assertThat(consumptionProbe1.getNanosToWaitForRefill()).isEqualTo(800_000_000L);

        EstimationProbe estimationProbe2 = bucket.estimateAbilityToConsume(3);
        assertThat(estimationProbe2.canBeConsumed()).isFalse();
        assertThat(estimationProbe2.getNanosToWaitForRefill()).isEqualTo(2_800_000_000L);
        ConsumptionProbe consumptionProbe2 = bucket.tryConsumeAndReturnRemaining(3);
        assertThat(consumptionProbe2.isConsumed()).isFalse();
        assertThat(consumptionProbe2.getNanosToWaitForRefill()).isEqualTo(2_800_000_000L);
    }

}
