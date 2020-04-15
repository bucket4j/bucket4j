package io.github.bucket4j.distributed.proxy.optimizers.batch.mock

import io.github.bucket4j.distributed.proxy.optimizers.batch.sync.BatchingExecutor
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
import org.junit.Test

@StressCTest(verifier = LinearizabilityVerifier::class)
@Param(name = "amount", gen = LongGen::class, conf = "1:20")
class BatchingExecutorLincheckTest  : VerifierState() {

    private val mockExecutor = MockCommandExecutor()
    private val executor = BatchingExecutor(mockExecutor)

    @Operation
    fun testBatching(@Param(name = "amount") amount: Long): Long {
        val cmd = MockCommand(amount)
        return executor.execute(cmd).data
    }

    @Test
    fun runTest() {
        val opts: Options<*, *> = StressOptions()
                .iterations(10)
                .threads(3)
                .minimizeFailedScenario(true)
                .logLevel(LoggingLevel.INFO)
        LinChecker.check(BatchingExecutorLincheckTest::class.java, opts)
    }

    override fun extractState(): Any {
        return mockExecutor.sum
    }

}