
package io.github.bucket4j.core_algorithms.integer

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.BucketState
import io.github.bucket4j.MathType
import io.github.bucket4j.TimeMeter
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration


class BucketStateSpecification extends Specification {

    @Shared
    private TimeMeter timeMeter = new TimeMeterMock()

    @Unroll
    def "GetAvailableTokens specification #testNumber"(String testNumber, long requiredAvailableTokens, Bucket bucket) {
        setup:
            BucketState state = bucket.asVerbose().getAvailableTokens().getState()
        when:
            long availableTokens = state.getAvailableTokens()
        then:
            availableTokens == requiredAvailableTokens
        where:
            [testNumber, requiredAvailableTokens, bucket] << [
                [
                        "#1",
                        10,
                        Bucket.builder()
                            .addLimit({it.capacity(10).refillGreedy(10, Duration.ofNanos(100))})
                            .withCustomTimePrecision(timeMeter)
                            .build()
                ], [
                        "#2",
                        0,
                        Bucket.builder()
                            .addLimit({it.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0)})
                            .withCustomTimePrecision(timeMeter)
                            .build()
                ], [
                        "#3",
                        5,
                        Bucket.builder()
                            .addLimit({it.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(5)})
                            .withCustomTimePrecision(timeMeter)
                            .build()
                ], [
                        "#4",
                        2,
                        Bucket.builder()
                            .addLimit({it.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(5)})
                            .addLimit({it.capacity(2).refillGreedy(2, Duration.ofNanos(100))})
                            .withCustomTimePrecision(timeMeter)
                            .build()
                ], [
                        "#5",
                        10,
                        Bucket.builder()
                            .addLimit(limit -> limit.capacity(10).refillGreedy(1, Duration.ofSeconds(1)))
                            .build()
                ]
            ]
    }

    @Unroll
    def "addTokens specification #testNumber"(String testNumber, long tokensToAdd, long requiredAvailableTokens, Bucket bucket) {
        setup:
            BucketState state = bucket.asVerbose().getAvailableTokens().getState()
        when:
            state.addTokens(tokensToAdd)
            long availableTokens = state.getAvailableTokens()
        then:
            availableTokens == requiredAvailableTokens
        where:
            [testNumber, tokensToAdd, requiredAvailableTokens, bucket] << [
                [
                    "#1",
                    10,
                    10,
                    Bucket.builder()
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0))
                        .withCustomTimePrecision(timeMeter)
                        .build()
                ], [
                    "#2",
                    1,
                    10,
                    Bucket.builder()
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)))
                        .withCustomTimePrecision(timeMeter)
                        .build()
                ], [
                    "#3",
                    6,
                    10,
                    Bucket.builder()
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(5))
                        .withCustomTimePrecision(timeMeter)
                        .build()
                ], [
                    "#4",
                    3,
                    2,
                    Bucket.builder()
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(5))
                        .addLimit(limit -> limit.capacity(2).refillGreedy(2, Duration.ofNanos(100)))
                        .withCustomTimePrecision(timeMeter)
                        .build()
                ], [
                    "#5",
                    4,
                    5,
                    Bucket.builder()
                        .addLimit(limit -> limit.capacity(10).refillGreedy(1, Duration.ofSeconds(1)).initialTokens(1))
                        .withCustomTimePrecision(timeMeter)
                        .build()
                ]
        ]
    }

    @Unroll
    def "delayAfterWillBePossibleToConsume specification #testNumber"(String testNumber, long toConsume, long requiredTime, Bucket bucket) {
        setup:
            TimeMeter timeMeter = bucket.timeMeter
            BucketState state = bucket.asVerbose().getAvailableTokens().getState()
        when:
            long actualTime = state.calculateDelayNanosAfterWillBePossibleToConsume(toConsume, timeMeter.currentTimeNanos(), false)
        then:
            actualTime == requiredTime
        where:
            [testNumber, toConsume, requiredTime, bucket] << [
                [
                    "#1",
                    10,
                    100,
                    Bucket.builder()
                        .withCustomTimePrecision(new TimeMeterMock(0))
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0))
                        .build()
                ], [
                    "#2",
                    10,
                    100,
                    Bucket.builder()
                        .withCustomTimePrecision(new TimeMeterMock(0))
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(0))
                        .build()
                ], [
                    "#3",
                    10,
                    500,
                    Bucket.builder()
                        .withCustomTimePrecision(new TimeMeterMock(0))
                        .addLimit(limit -> limit.capacity(10).refillGreedy(2, Duration.ofNanos(100)).initialTokens(0))
                        .build()
                ], [
                    "#4",
                    7,
                    30,
                    Bucket.builder()
                        .withCustomTimePrecision(new TimeMeterMock(0))
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(4))
                        .build()
                ], [
                    "#5",
                    11,
                    70,
                    Bucket.builder()
                        .withCustomTimePrecision(new TimeMeterMock(0))
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(4))
                        .build()
                ], [
                    "#6",
                    3,
                    20,
                    Bucket.builder()
                        .withCustomTimePrecision(new TimeMeterMock(0))
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1))
                        .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofNanos(10)).initialTokens(2))
                        .build()
                ], [
                    "#7",
                    3,
                    20,
                    Bucket.builder()
                        .withCustomTimePrecision(new TimeMeterMock(0))
                        .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofNanos(10)).initialTokens(2))
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1))
                        .build()
                ], [
                    "#8",
                    3,
                    0,
                    Bucket.builder()
                        .withCustomTimePrecision(new TimeMeterMock(0))
                        .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofNanos(10)).initialTokens(5))
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(3))
                        .build()
                ]
            ]
    }

    @Unroll
    def "calculateFullRefillingTime specification #testNumber"(String testNumber, long requiredTime,
                                                               long timeShiftBeforeAsk, long tokensConsumeBeforeAsk, BucketConfiguration configuration) {
        setup:
            BucketState state = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, 0L)
            state.refillAllBandwidth(timeShiftBeforeAsk)
            state.consume(tokensConsumeBeforeAsk)
        when:
            long actualTime = state.calculateFullRefillingTime(timeShiftBeforeAsk)
        then:
            actualTime == requiredTime
        where:
            [testNumber, requiredTime, timeShiftBeforeAsk, tokensConsumeBeforeAsk, configuration] << [
                [
                        "#1",
                        90,
                        0,
                        0,
                        BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1))
                            .build()
                ], [
                        "#2",
                        100,
                        0,
                        0,
                        BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(10).refillIntervally(10, Duration.ofNanos(100)).initialTokens(1))
                            .build()
                ], [
                        "#3",
                        1650,
                        0,
                        23,
                        BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(10).refillGreedy(2, Duration.ofNanos(100)).initialTokens(0))
                            .build()
                ], [
                        "#4",
                        1700,
                        0,
                        23,
                        BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(10).refillIntervally(2, Duration.ofNanos(100)).initialTokens(0))
                            .build()
                ], [
                        "#5",
                        60,
                        0,
                        0,
                        BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(4))
                            .build()
                ], [
                        "#6",
                        90,
                        0,
                        0,
                        BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1))
                            .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofNanos(10)).initialTokens(2))
                            .build()
                ], [
                        "#7",
                        90,
                        0,
                        0,
                        BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofNanos(10)).initialTokens(2))
                            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(1))
                            .build()
                ], [
                        "#8",
                        70,
                        0,
                        0,
                        BucketConfiguration.builder()
                            .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofNanos(10)).initialTokens(5))
                            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofNanos(100)).initialTokens(3))
                            .build()
                ]
        ]
    }

    @Unroll
    def "Specification for refill simple bandwidth #n"(int n, long initialTokens, long capacity, long period,
                                                       long initTime, long timeOnRefill, long tokensAfterRefill, long roundingError) {
        setup:
            TimeMeterMock mockTimer = new TimeMeterMock(initTime)
            Bucket bucket = Bucket.builder()
                    .addLimit({
                        limit -> limit.capacity(capacity)
                            .refillGreedy(capacity, Duration.ofNanos(period))
                            .initialTokens(initialTokens)
                    })
                    .withCustomTimePrecision(mockTimer)
                    .build()
            BucketState state = bucket.asVerbose().getAvailableTokens().getState()
        when:
            mockTimer.setCurrentTimeNanos(timeOnRefill)
            state.refillAllBandwidth(timeOnRefill)
        then:
            state.getCurrentSize(0) == tokensAfterRefill
            state.getRoundingError(0) == roundingError
        where:
        n  | initialTokens |    capacity    | period | initTime | timeOnRefill | tokensAfterRefill | roundingError
        1  |        0      |      1000      | 1000   | 10000    |     10040    |       40          |      0
        2  |       50      |      1000      | 1000   | 10000    |     10001    |       51          |      0
        3  |       55      |      1000      | 1000   | 10000    |      9999    |       55          |      0
        4  |      200      |      1000      | 1000   | 10000    |     20000    |     1000          |      0
        5  |        0      |       100      | 1000   | 10000    |     10003    |        0          |      300
        6  |       90      |       100      | 1000   | 10000    |     10017    |       91          |      700
        7  |        0      |       100      | 1000   | 10000    |     28888    |      100          |      0
    }

    @Unroll
    def "Specification for refill classic bandwidth #n"(int n, long initialTokens, long capacity, long refillTokens, long refillPeriod,
                                                       long initTime, long timeOnRefill, long tokensAfterRefill, long roundingError) {
        setup:
            TimeMeterMock mockTimer = new TimeMeterMock(initTime)
            Bucket bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(refillTokens, Duration.ofNanos(refillPeriod)).initialTokens(initialTokens))
                .withCustomTimePrecision(mockTimer)
                .build()
            BucketState state = bucket.asVerbose().getAvailableTokens().getState()
        when:
            mockTimer.setCurrentTimeNanos(timeOnRefill)
            state.refillAllBandwidth(timeOnRefill)
        then:
            state.getCurrentSize(0) == tokensAfterRefill
            state.getRoundingError(0) == roundingError
        where:
        n  | initialTokens |    capacity    | refillTokens | refillPeriod | initTime | timeOnRefill | tokensAfterRefill | roundingError
        1  |        0      |      1000      |       1      |          1   | 10000    |     10040    |       40          |      0
        2  |       50      |      1000      |      10      |         10   | 10000    |     10001    |       51          |      0
        3  |       55      |      1000      |       1      |          1   | 10000    |     10000    |       55          |      0
        4  |      200      |      1000      |      10      |         10   | 10000    |     20000    |     1000          |      0
        5  |        0      |       100      |       1      |         10   | 10000    |     10003    |        0          |      3
        6  |       90      |       100      |       1      |         10   | 10000    |     10017    |       91          |      7
        7  |        0      |       100      |       1      |         10   | 10000    |     28888    |      100          |      0
    }

    @Unroll
    def "Specification for consume #n"(int n, long initialTokens, long period,
                                       long capacity, long toConsume, long requiredSize
                                       ) {
        setup:
            Bucket bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, Duration.ofNanos(period)).initialTokens(initialTokens))
                .withCustomTimePrecision(new TimeMeterMock(0))
                .build()
            BucketState state = bucket.asVerbose().getAvailableTokens().getState()
        when:
            state.consume(toConsume)
        then:
            state.getCurrentSize(0) == requiredSize
        where:
        n  |  initialTokens  | period | capacity | toConsume | requiredSize
        1  |        0        | 1000   |   1000   |    10     |   -10
        2  |       50        | 1000   |   1000   |     2     |    48
        3  |       55        | 1000   |   1000   |   1600    |   -1545
    }

}
