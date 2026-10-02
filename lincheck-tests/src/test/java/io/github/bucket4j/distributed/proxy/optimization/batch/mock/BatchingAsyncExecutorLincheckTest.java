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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Covers only the happy path: every command succeeds, so this test can never observe a hang caused by an
 * uncaught exception on the failure path. See {@link io.github.bucket4j.util.concurrent.batch.AsyncBatchHelperSynchronousFailureLincheckTest}
 * and {@link io.github.bucket4j.util.concurrent.batch.AsyncBatchHelperResultSplitterFailureLincheckTest} for
 * dedicated failure-path coverage.
 */
@StressCTest(verifier = LinearizabilityVerifier.class)
@Param(name = "amount", gen = LongGen.class, conf = "1:20")
public class BatchingAsyncExecutorLincheckTest extends VerifierState {

    private final MockBatchExecutor mockExecutor = new MockBatchExecutor();

    @Operation
    public long testBatching(@Param(name = "amount") long amount) throws ExecutionException, InterruptedException {
        SingleMockCommand cmd = new SingleMockCommand(amount);
        CompletableFuture<Long> future = mockExecutor.getAsyncBatchHelper().executeAsync(cmd);
        return future.get();
    }

    @Test
    @Timeout(60)
    public void runTest() {
        Options<?, ?> opts = new StressOptions()
                .iterations(10)
                .threads(3)
                .minimizeFailedScenario(true)
                .logLevel(LoggingLevel.INFO);
        LinChecker.check(BatchingAsyncExecutorLincheckTest.class, opts);
    }

    @Override
    protected Object extractState() {
        return mockExecutor.getState().getSum();
    }

}
