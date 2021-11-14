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
            BucketState state = BucketState.createInitialState(configuration, currentTimeNanos)
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

}
