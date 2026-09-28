package io.github.bucket4j.util.concurrent.batch

import io.github.bucket4j.util.concurrent.batch.AsyncBatchHelper
import org.jetbrains.kotlinx.lincheck.LinChecker
import org.jetbrains.kotlinx.lincheck.LoggingLevel
import org.jetbrains.kotlinx.lincheck.Options
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.paramgen.LongGen
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressCTest
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState
import org.jetbrains.kotlinx.lincheck.verifier.linearizability.LinearizabilityVerifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ExecutionException

/**
 * Reproduces the bug where a synchronous (as opposed to a failed-future) exception thrown from the
 * solo-execution path of [AsyncBatchHelper.executeAsync] left the internal lock permanently acquired,
 * hanging every task enqueued afterwards. See [io.github.bucket4j.util.concurrent.AsyncBatchHelperTest.testThatSynchronousExceptionInSoloExecutionDoesNotHangSubsequentTasks]
 * for the deterministic single-threaded reproduction; this test stresses the same code path concurrently.
 *
 * The combined executor always throws synchronously, so every operation - whether it takes the solo
 * path or ends up batched with concurrently-running operations - deterministically fails with the same
 * exception type. This is required for soundness under Lincheck's default sequential model, which always
 * replays operations one at a time via the solo path: if the outcome depended on whether an operation
 * happened to be batched, sequential replay and concurrent execution could diverge on outcome, producing
 * a false-positive linearizability violation unrelated to any real bug.
 */
@StressCTest(verifier = LinearizabilityVerifier::class)
@Param(name = "amount", gen = LongGen::class, conf = "1:20")
class AsyncBatchHelperSynchronousFailureLincheckTest : VerifierState() {

    private val poison = IllegalStateException("synchronous failure from backend")

    private val helper: AsyncBatchHelper<Long, Long, List<Long>, List<Long>> = AsyncBatchHelper.create(
            { tasks: List<Long> -> tasks },
            { _: List<Long> -> throw poison },
            { _: List<Long>, combinedResult: List<Long> -> combinedResult }
    )

    @Operation(handleExceptionsAsResult = [IllegalStateException::class])
    fun executeFailing(@Param(name = "amount") amount: Long): Long {
        try {
            return helper.executeAsync(amount).get()
        } catch (e: ExecutionException) {
            throw (e.cause as? IllegalStateException) ?: IllegalStateException(e.cause)
        }
    }

    @Test
    @Timeout(120)
    fun runTest() {
        val opts: Options<*, *> = StressOptions()
                .iterations(10)
                .invocationsPerIteration(200)
                .threads(4)
                .actorsPerThread(4)
                .minimizeFailedScenario(true)
                .logLevel(LoggingLevel.INFO)
        LinChecker.check(AsyncBatchHelperSynchronousFailureLincheckTest::class.java, opts)
    }

    override fun extractState(): Any {
        // every operation always throws the same exception type and never mutates any observable state
        return 0
    }

}
