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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

/**
 * Reproduces the bug where [combinedResultSplitter] throwing after a *successful* combined execution left
 * every waiting task in that batch without its future ever being completed, hanging their callers forever.
 * See [io.github.bucket4j.util.concurrent.AsyncBatchHelperTest.testThatResultSplitterExceptionOnSuccessfulBatchDoesNotHangOtherWaitingTasks]
 * for the deterministic single-threaded reproduction; this test stresses the same code path concurrently.
 *
 * The combined executor always succeeds, but the splitter always throws unconditionally - regardless of
 * whether the batch it is unwrapping has one element (solo path) or several (batched path). This is
 * required for soundness under Lincheck's default sequential model: an earlier draft of this test made the
 * splitter throw only when the batch size exceeded one, which is exactly the kind of path-dependent
 * behavior that diverges between sequential replay (always solo, size 1, thus never throws) and concurrent
 * runs (sometimes batched, thus sometimes throws) - producing a false-positive linearizability violation
 * on otherwise-correct code. Throwing unconditionally keeps every operation's outcome identical regardless
 * of path.
 */
@StressCTest(verifier = LinearizabilityVerifier::class)
@Param(name = "amount", gen = LongGen::class, conf = "1:20")
class AsyncBatchHelperResultSplitterFailureLincheckTest : VerifierState() {

    private val splitterError = IllegalStateException("bug while unwrapping combined result")

    private val helper: AsyncBatchHelper<Long, Long, List<Long>, List<Long>> = AsyncBatchHelper.create(
            { tasks: List<Long> -> tasks },
            { tasks: List<Long> -> CompletableFuture.completedFuture(tasks) },
            { _: List<Long>, _: List<Long> -> throw splitterError }
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
        LinChecker.check(AsyncBatchHelperResultSplitterFailureLincheckTest::class.java, opts)
    }

    override fun extractState(): Any {
        // every operation always throws the same exception type and never mutates any observable state
        return 0
    }

}
