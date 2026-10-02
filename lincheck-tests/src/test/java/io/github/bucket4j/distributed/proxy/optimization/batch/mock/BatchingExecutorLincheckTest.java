package io.github.bucket4j.distributed.proxy.optimization.batch.mock;

import org.jetbrains.kotlinx.lincheck.LinChecker;
import org.jetbrains.kotlinx.lincheck.LoggingLevel;
import org.jetbrains.kotlinx.lincheck.Options;
import org.jetbrains.kotlinx.lincheck.annotations.Operation;
import org.jetbrains.kotlinx.lincheck.annotations.Param;
import org.jetbrains.kotlinx.lincheck.paramgen.LongGen;
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressCTest;
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions;
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState;
import org.jetbrains.kotlinx.lincheck.verifier.linearizability.LinearizabilityVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Covers only the happy path: every command succeeds, so this test can never observe a hang caused by an
 * uncaught exception on the failure path. `BatchHelper` itself has no known equivalent bugs (unlike
 * `AsyncBatchHelper`), so no dedicated failure-path Lincheck test was added for it.
 */
@StressCTest(verifier = LinearizabilityVerifier.class)
@Param(name = "amount", gen = LongGen.class, conf = "1:20")
public class BatchingExecutorLincheckTest extends VerifierState {

    private final MockBatchExecutor mockExecutor = new MockBatchExecutor();

    @Operation
    public long testBatching(@Param(name = "amount") long amount) {
        SingleMockCommand cmd = new SingleMockCommand(amount);
        return mockExecutor.getSyncBatchHelper().execute(cmd);
    }

    @Test
    @Timeout(60)
    public void runTest() {
        Options<?, ?> opts = new StressOptions()
                .iterations(10)
                .threads(3)
                .minimizeFailedScenario(true)
                .logLevel(LoggingLevel.INFO);
        LinChecker.check(BatchingExecutorLincheckTest.class, opts);
    }

    @Override
    protected Object extractState() {
        return mockExecutor.getState().getSum();
    }

}
