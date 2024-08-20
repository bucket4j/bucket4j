
package io.github.bucket4j.core_algorithms


import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.EstimationProbe
import io.github.bucket4j.mock.BucketType
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class BucketRoundingRulesSpecification extends Specification {

    def "rest of division should not be missed on next consumption"() {
        setup:
            TimeMeterMock meter = new TimeMeterMock(0)
        Bucket bucket = Bucket.builder()
            .withCustomTimePrecision(meter)
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0))
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
            Bucket bucket = Bucket.builder()
                .withCustomTimePrecision(meter)
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0)})
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

    @Unroll
    def "Partially refilled token should not be missed when calculating time for refill #bucketType"(BucketType bucketType) {
        expect:
            def timeMeter = new TimeMeterMock(0)
            def configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(4).refillGreedy(1, Duration.ofSeconds(1)).initialTokens(1))
                .build()
            Bucket bucket = bucketType.createBucket(configuration, timeMeter)

            assert bucket.tryConsumeAsMuchAsPossible() == 1

            timeMeter.addTime(200_000_000)

        EstimationProbe estimationProbe1 = bucket.estimateAbilityToConsume(1)
            assert !estimationProbe1.canBeConsumed()
            assert estimationProbe1.nanosToWaitForRefill == 800_000_000
        ConsumptionProbe consumptionProbe1 = bucket.tryConsumeAndReturnRemaining(1)
            assert !consumptionProbe1.consumed
            assert consumptionProbe1.nanosToWaitForRefill == 800_000_000

            EstimationProbe estimationProbe2 = bucket.estimateAbilityToConsume(3)
            assert !estimationProbe2.canBeConsumed()
            assert estimationProbe2.nanosToWaitForRefill == 2_800_000_000
            ConsumptionProbe consumptionProbe2 = bucket.tryConsumeAndReturnRemaining(3)
            assert !consumptionProbe2.consumed
            assert consumptionProbe2.nanosToWaitForRefill == 2_800_000_000
        where:
            bucketType << BucketType.values()
    }

}
