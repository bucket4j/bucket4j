package io.github.bucket4j.util.concurrent.batch;

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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Reproduces the bug where the combined result splitter throwing after a *successful* combined execution left
 * every waiting task in that batch without its future ever being completed, hanging their callers forever.
 * See {@code io.github.bucket4j.util.concurrent.AsyncBatchHelperTest#testThatResultSplitterExceptionOnSuccessfulBatchDoesNotHangOtherWaitingTasks}
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
@StressCTest(verifier = LinearizabilityVerifier.class)
@Param(name = "amount", gen = LongGen.class, conf = "1:20")
public class AsyncBatchHelperResultSplitterFailureLincheckTest extends VerifierState {

    private final IllegalStateException splitterError = new IllegalStateException("bug while unwrapping combined result");

    private final AsyncBatchHelper<Long, Long, List<Long>, List<Long>> helper = AsyncBatchHelper.create(
            tasks -> tasks,
            tasks -> CompletableFuture.completedFuture(tasks),
            (tasks, combinedResult) -> {
                throw splitterError;
            }
    );

    @Operation(handleExceptionsAsResult = IllegalStateException.class)
    public long executeFailing(@Param(name = "amount") long amount) throws InterruptedException {
        try {
            return helper.executeAsync(amount).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalStateException) {
                throw (IllegalStateException) e.getCause();
            }
            throw new IllegalStateException(e.getCause());
        }
    }

    @Test
    @Timeout(120)
    public void runTest() {
        Options<?, ?> opts = new StressOptions()
                .iterations(10)
                .invocationsPerIteration(200)
                .threads(4)
                .actorsPerThread(4)
                .minimizeFailedScenario(true)
                .logLevel(LoggingLevel.INFO);
        LinChecker.check(AsyncBatchHelperResultSplitterFailureLincheckTest.class, opts);
    }

    @Override
    protected Object extractState() {
        // every operation always throws the same exception type and never mutates any observable state
        return 0;
    }

}
