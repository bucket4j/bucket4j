
package io.github.bucket4j.core_algorithms

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.MathType
import io.github.bucket4j.Refill
import io.github.bucket4j.local.LocalBucket
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

import static io.github.bucket4j.MathType.*


class IntervallyAlignedRefillSpecification extends Specification {

    @Unroll
    def "#n Initial token calculation spec"(int n, long currentTimeMillis, long firstRefillTimeMillis, long capacity,
                                            long refillTokens, long refillPeriodMillis, long requiredInitialTokens, MathType mathType) {
        setup:
            Instant firstRefillTime = new Date(firstRefillTimeMillis).toInstant()
            Duration refillPeriod = Duration.ofMillis(refillPeriodMillis)
        Refill refill = Refill.intervallyAligned(refillTokens, refillPeriod, firstRefillTime, true)
        Bandwidth bandwidth = Bandwidth.classic(capacity, refill)
            TimeMeterMock mockTimer = new TimeMeterMock(currentTimeMillis * 1_000_000)

        Bucket bucket = Bucket.builder()
                    .withCustomTimePrecision(mockTimer)
                    .addLimit(bandwidth)
                    .withMath(mathType)
                    .build()

        expect:
            assert bucket.getAvailableTokens() == requiredInitialTokens

        where:
            n  | currentTimeMillis | firstRefillTimeMillis | capacity  |  refillTokens | refillPeriodMillis | requiredInitialTokens | mathType
            0  |      60_000       |        60_000         |    400    |       400     |       60_000       |          400          | INTEGER_64_BITS
            1  |      20_000       |        60_000         |    400    |       400     |       60_000       |          266          | INTEGER_64_BITS
            2  |      20_000       |        60_000         |    400    |       300     |       60_000       |          300          | INTEGER_64_BITS
            3  |      20_000       |        60_000         |    400    |       200     |       60_000       |          333          | INTEGER_64_BITS
            4  |      20_000       |        60_000         |    400    |       100     |       60_000       |          366          | INTEGER_64_BITS
            5  |      60_000       |        60_000         |    400    |       100     |       60_000       |          400          | INTEGER_64_BITS
            6  |      60_001       |        60_000         |    400    |       100     |       60_000       |          400          | INTEGER_64_BITS
            7  |      20_000       |       180_000         |    400    |       100     |       60_000       |          400          | INTEGER_64_BITS
            8  |      20_000       |        60_000         |    100    |       400     |       60_000       |          100          | INTEGER_64_BITS
            9  |      59_000       |        60_000         |    100    |       400     |       60_000       |          6            | INTEGER_64_BITS
    }

    @Unroll
    def "complex spec for case when useAdaptiveInitialTokens=false mathType = #mathType"(MathType mathType) {
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

            Bucket bucket = Bucket.builder()
                .withCustomTimePrecision(mockTimer)
                .addLimit(bandwidth)
                .withMath(mathType)
                .build()

        expect: "initialTokens == capacity because useAdaptiveInitialTokens == false"
            assert bucket.getAvailableTokens() == 400

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
            bucket.consumeIgnoringRateLimits(401) == TimeUnit.MINUTES.toNanos(3)

        where:
            mathType << MathType.values()
    }

    @Unroll
    def "complex spec for case when useAdaptiveInitialTokens=true mathType = #mathType"(MathType mathType) {
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

            Bucket bucket = Bucket.builder()
                    .withCustomTimePrecision(mockTimer)
                    .addLimit(bandwidth)
                    .withMath(mathType)
                    .build()

        expect: "initialTokens == capacity because useAdaptiveInitialTokens == false"
            assert bucket.getAvailableTokens() == 333

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
            bucket.consumeIgnoringRateLimits(401) == TimeUnit.MINUTES.toNanos(3)

        where:
            mathType << MathType.values()
    }

    @Unroll
    def "check that boundary of interval is not missed during long time of inactivity mathType = #mathType"(MathType mathType) {
        setup:
            TimeMeterMock timeMeter = new TimeMeterMock(TimeUnit.MILLISECONDS.toNanos(103))

            Refill refill = Refill.intervallyAligned(400, Duration.ofMillis(100), new Date(200).toInstant(), false);
            LocalBucket bucket = Bucket.builder()
                    .withCustomTimePrecision(timeMeter)
                    .addLimit(Bandwidth.classic(400, refill))
                    .withMath(mathType)
                    .build()

            bucket.tryConsumeAsMuchAsPossible()

        when: "when bucket was inactive for significant time and consumption resumed something int the middle of interval"
           timeMeter.setCurrentTimeMillis(32849)
        then: "bucket should be refilled to max capacity"
           bucket.getAvailableTokens() == 400
        and: "original boundary of interval should not be missed, next refill date should be chosen based on original boundary instead of current time"
           bucket.tryConsumeAsMuchAsPossible() == 400
           bucket.getAvailableTokens() == 0
           timeMeter.addMillis(51)
           bucket.getAvailableTokens() == 400

        where:
            mathType << MathType.values()
    }

}
