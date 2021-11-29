package io.github.bucket4j

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class VerboseApiTest extends Specification {

    @Unroll
    def "calculateFullRefillingTime specification #testNumber"(String testNumber, long requiredTime,
                                                               long timeShiftBeforeAsk, long tokensConsumeBeforeAsk, BucketConfiguration configuration) {
        setup:
            long currentTimeNanos = 0L
            BucketState state = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS,  currentTimeNanos)
            state.refillAllBandwidth(configuration.bandwidths, timeShiftBeforeAsk)
            state.consume(configuration.bandwidths, tokensConsumeBeforeAsk)
            VerboseResult verboseResult = new VerboseResult(timeShiftBeforeAsk, 42, configuration, state)

        when:
            long actualTime = verboseResult.getDiagnostics().calculateFullRefillingTime()
        then:
            actualTime == requiredTime
        where:
            [testNumber, requiredTime, timeShiftBeforeAsk, tokensConsumeBeforeAsk, configuration] << [
                [
                        "#1",
                        90,
                        0,
                        0,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
                                .build()
                ], [
                        "#2",
                        100,
                        0,
                        0,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofNanos(100))).withInitialTokens(1))
                                .build()
                ], [
                        "#3",
                        1650,
                        0,
                        23,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.classic(10, Refill.greedy(2, Duration.ofNanos(100))).withInitialTokens(0))
                                .build()
                ], [
                        "#4",
                        1700,
                        0,
                        23,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.classic(10, Refill.intervally(2, Duration.ofNanos(100))).withInitialTokens(0))
                                .build()
                ], [
                        "#5",
                        60,
                        0,
                        0,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(4))
                                .build()
                ], [
                        "#6",
                        90,
                        0,
                        0,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
                                .addLimit(Bandwidth.simple(5, Duration.ofNanos(10)).withInitialTokens(2))
                                .build()
                ], [
                        "#7",
                        90,
                        0,
                        0,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.simple(5, Duration.ofNanos(10)).withInitialTokens(2))
                                .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(1))
                                .build()
                ], [
                        "#8",
                        70,
                        0,
                        0,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.simple(5, Duration.ofNanos(10)).withInitialTokens(5))
                                .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(3))
                                .build()
                ]
            ]
    }

    def "getAvailableTokens"(String testNumber, long requiredAvailableTokens, BucketConfiguration configuration) {
        setup:
            long currentTimeNanos = 0L
            BucketState state = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS,  currentTimeNanos)
            VerboseResult verboseResult = new VerboseResult(currentTimeNanos, 42, configuration, state)
        when:
            long availableTokens = verboseResult.getDiagnostics().getAvailableTokens()
        then:
            availableTokens == requiredAvailableTokens
        where:
            [testNumber, requiredAvailableTokens, configuration] << [
                [
                        "#1",
                        3,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(3))
                                .build()
                ], [
                        "#2",
                        10,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofNanos(100))))
                                .build()
                ], [
                        "#3",
                        1,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.classic(10, Refill.greedy(2, Duration.ofNanos(100))).withInitialTokens(1))
                                .addLimit(Bandwidth.classic(100, Refill.greedy(20, Duration.ofNanos(100))))
                                .build()
                ]
        ]
    }

    @Unroll
    def "getAvailableTokensPerEachBandwidth"(String testNumber, List<Long> requiredAvailableTokens, BucketConfiguration configuration) {
        setup:
            long currentTimeNanos = 0L
            BucketState state = BucketState.createInitialState(configuration, MathType.INTEGER_64_BITS, currentTimeNanos)
            VerboseResult verboseResult = new VerboseResult(currentTimeNanos, 42, configuration, state)
        when:
            long[] availableTokens = verboseResult.getDiagnostics().getAvailableTokensPerEachBandwidth()
        then:
            Arrays.asList(availableTokens) == requiredAvailableTokens
        where:
            [testNumber, requiredAvailableTokens, configuration] << [
                [
                        "#1",
                        [ 3l ] as List,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.simple(10, Duration.ofNanos(100)).withInitialTokens(3))
                                .build()
                ], [
                        "#2",
                        [ 10l ] as List,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofNanos(100))))
                                .build()
                ], [
                        "#3",
                        [ 1l, 100l ] as List,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.classic(10, Refill.greedy(2, Duration.ofNanos(100))).withInitialTokens(1))
                                .addLimit(Bandwidth.classic(100, Refill.greedy(20, Duration.ofNanos(100))))
                                .build()
                ], [
                        "#3",
                        [ 100l, 1l ] as List,
                        Bucket4j.configurationBuilder()
                                .addLimit(Bandwidth.classic(100, Refill.greedy(20, Duration.ofNanos(100))))
                                .addLimit(Bandwidth.classic(10, Refill.greedy(2, Duration.ofNanos(100))).withInitialTokens(1))
                                .build()
                ]
        ]
    }

}
