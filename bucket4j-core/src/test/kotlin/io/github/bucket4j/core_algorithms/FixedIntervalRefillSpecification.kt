package io.github.bucket4j.core_algorithms

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.mock.TimeMeterMock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.concurrent.TimeUnit

class FixedIntervalRefillSpecification : FunSpec({

    test("basic test of fixed interval refill") {
        val bandwidth = Bandwidth.builder()
            .capacity(9)
            .refillIntervally(9, Duration.ofNanos(10))
            .initialTokens(0)
            .build()
        val mockTimer = TimeMeterMock(0)
        val bucket: Bucket = Bucket.builder()
            .withCustomTimePrecision(mockTimer)
            .addLimit(bandwidth)
            .build()

        bucket.availableTokens shouldBeExactly 0

        mockTimer.addTime(4)
        bucket.availableTokens shouldBeExactly 0

        mockTimer.addTime(6)
        bucket.availableTokens shouldBeExactly 9

        mockTimer.addTime(1)
        bucket.availableTokens shouldBeExactly 9
    }

    test("complex test of fixed interval refill") {
        val bandwidth1 = Bandwidth.builder()
            .capacity(9)
            .refillIntervally(5, Duration.ofNanos(6))
            .initialTokens(0)
            .build()
        val bandwidth2 = Bandwidth.builder()
            .capacity(12)
            .refillIntervally(4, Duration.ofNanos(5))
            .initialTokens(0)
            .build()
        val mockTimer = TimeMeterMock(0)
        val bucket: Bucket = Bucket.builder()
            .withCustomTimePrecision(mockTimer)
            .addLimit(bandwidth1)
            .addLimit(bandwidth2)
            .build()

        bucket.availableTokens shouldBeExactly 0

        mockTimer.addTime(4) // 4
        bucket.availableTokens shouldBeExactly 0

        mockTimer.addTime(1) // 5
        bucket.availableTokens shouldBeExactly 0

        mockTimer.addTime(1) // 6
        bucket.availableTokens shouldBeExactly 4

        mockTimer.addTime(4) // 10
        bucket.availableTokens shouldBeExactly 5

        mockTimer.addTime(2) // 12
        bucket.availableTokens shouldBeExactly 8

        mockTimer.addTime(3) // 15
        bucket.availableTokens shouldBeExactly 9
    }

    test("refill time estimation, https://github.com/bucket4j/bucket4j/issues/71") {
        val bandwidth = Bandwidth.builder()
            .capacity(10)
            .refillIntervally(10, Duration.ofMinutes(1))
            .initialTokens(0)
            .build()
        val mockTimer = TimeMeterMock(0)
        val bucket: Bucket = Bucket.builder()
            .withCustomTimePrecision(mockTimer)
            .addLimit(bandwidth)
            .build()

        var probe = bucket.tryConsumeAndReturnRemaining(1)
        probe.isConsumed shouldBe false
        probe.remainingTokens shouldBeExactly 0
        probe.nanosToWaitForRefill shouldBeExactly TimeUnit.SECONDS.toNanos(60)

        probe = bucket.tryConsumeAndReturnRemaining(10)
        probe.isConsumed shouldBe false
        probe.remainingTokens shouldBeExactly 0
        probe.nanosToWaitForRefill shouldBeExactly TimeUnit.SECONDS.toNanos(60)

        mockTimer.addTime(TimeUnit.SECONDS.toNanos(15))
        probe = bucket.tryConsumeAndReturnRemaining(1)
        probe.isConsumed shouldBe false
        probe.remainingTokens shouldBeExactly 0
        probe.nanosToWaitForRefill shouldBeExactly TimeUnit.SECONDS.toNanos(45)

        probe = bucket.tryConsumeAndReturnRemaining(15)
        probe.isConsumed shouldBe false
        probe.remainingTokens shouldBeExactly 0
        probe.nanosToWaitForRefill shouldBeExactly Long.MAX_VALUE
    }
})
