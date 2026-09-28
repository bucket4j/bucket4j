package io.github.bucket4j.distributed.proxy.optimization.batch.mock

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

/**
 * Covers only the happy path: every command succeeds, so this test can never observe a hang caused by an
 * uncaught exception on the failure path. See [io.github.bucket4j.util.concurrent.batch.AsyncBatchHelperSynchronousFailureLincheckTest]
 * and [io.github.bucket4j.util.concurrent.batch.AsyncBatchHelperResultSplitterFailureLincheckTest] for
 * dedicated failure-path coverage.
 */
@StressCTest(verifier = LinearizabilityVerifier::class)
@Param(name = "amount", gen = LongGen::class, conf = "1:20")
class BatchingAsyncExecutorLincheckTest : VerifierState() {

    private val mockExecutor = MockBatchExecutor()

    @Operation
    fun testBatching(@Param(name = "amount") amount: Long) :Long {
        val cmd = SingleMockCommand(amount)
        val future: CompletableFuture<Long> = mockExecutor.asyncBatchHelper.executeAsync(cmd)
        return future.get()
    }

    @Test
    @Timeout(60)
    fun runTest() {
        val opts: Options<*, *> = StressOptions()
                .iterations(10)
                .threads(3)
                .minimizeFailedScenario(true)
                .logLevel(LoggingLevel.INFO)
        LinChecker.check(BatchingAsyncExecutorLincheckTest::class.java, opts)
    }

    override fun extractState(): Any {
        return mockExecutor.state.sum
    }

}