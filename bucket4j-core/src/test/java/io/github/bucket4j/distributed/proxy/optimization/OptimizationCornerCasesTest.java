package io.github.bucket4j.distributed.proxy.optimization;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.optimization.delay.DelayOptimization;
import io.github.bucket4j.distributed.proxy.optimization.manual.ManuallySyncingOptimization;
import io.github.bucket4j.distributed.proxy.optimization.predictive.PredictiveOptimization;
import io.github.bucket4j.distributed.proxy.optimization.skiponzero.SkipSyncOnZeroOptimization;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.remote.commands.CheckConfigurationVersionAndExecuteCommand;
import io.github.bucket4j.distributed.remote.commands.CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand;
import io.github.bucket4j.distributed.remote.commands.GetAvailableTokensCommand;
import io.github.bucket4j.distributed.remote.commands.SyncCommand;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.mock.CompareAndSwapBasedProxyManagerMock;
import io.github.bucket4j.mock.LockBasedProxyManagerMock;
import io.github.bucket4j.mock.ProxyManagerMock;
import io.github.bucket4j.mock.SelectForUpdateBasedProxyManagerMock;
import io.github.bucket4j.mock.TimeMeterMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OptimizationCornerCases Specification")
class OptimizationCornerCasesTest {

    private record OptimizationCase(int testNumber, Optimization optimization) {

        @Override
        public String toString() {
            return "#" + testNumber;
        }
    }

    static Stream<OptimizationCase> optimizations() {
        return Stream.of(
            new OptimizationCase(1, Optimizations.batching()),
            new OptimizationCase(2, new DelayOptimization(DELAY_PARAMETERS, NopeOptimizationListener.INSTANCE, CLOCK)),
            new OptimizationCase(3, new PredictiveOptimization(PredictionParameters.createDefault(DELAY_PARAMETERS), DELAY_PARAMETERS, NopeOptimizationListener.INSTANCE, CLOCK)),
            new OptimizationCase(4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, CLOCK)),
            new OptimizationCase(5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, CLOCK))
        );
    }

    private static final DelayParameters DELAY_PARAMETERS = new DelayParameters(1, Duration.ofNanos(1));

    private static final TimeMeterMock CLOCK = new TimeMeterMock();

    // https://github.com/bucket4j/bucket4j/issues/398
    @ParameterizedTest
    @MethodSource("optimizations")
    void shouldCorrectlyHandleExceptionsWhenOptimizationIsUsedWithProxyManagerMock(OptimizationCase testCase) throws Exception {
        ProxyManagerMock proxyManagerMock = new ProxyManagerMock(CLOCK);
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();

        BucketProxy bucket = proxyManagerMock.builder()
            .withOptimization(testCase.optimization())
            .build("66", configuration);

        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume(1)).isTrue();
        }
        proxyManagerMock.removeProxy("66");

        bucket.forceAddTokens(90);
        assertThat(bucket.getAvailableTokens()).isEqualTo(100);

        proxyManagerMock.removeProxy("66");

        bucket.asVerbose().forceAddTokens(90);
        assertThat(bucket.asVerbose().getAvailableTokens().getValue()).isEqualTo(100);
    }

    // https://github.com/bucket4j/bucket4j/issues/398
    @ParameterizedTest
    @MethodSource("optimizations")
    void shouldCorrectlyHandleExceptionsWhenOptimizationIsUsedWithCompareAndSwapBasedProxyManagerMock(OptimizationCase testCase) throws Exception {
        CompareAndSwapBasedProxyManagerMock proxyManagerMock = new CompareAndSwapBasedProxyManagerMock(ClientSideConfig.getDefault().withClientClock(CLOCK));
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();

        BucketProxy bucket = proxyManagerMock.builder()
            .withOptimization(testCase.optimization())
            .build("66", configuration);

        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume(1)).isTrue();
        }
        proxyManagerMock.removeProxy("66");

        bucket.forceAddTokens(90);
        assertThat(bucket.getAvailableTokens()).isEqualTo(100);

        proxyManagerMock.removeProxy("66");

        bucket.asVerbose().forceAddTokens(90);
        assertThat(bucket.asVerbose().getAvailableTokens().getValue()).isEqualTo(100);
    }

    // https://github.com/bucket4j/bucket4j/issues/398
    @ParameterizedTest
    @MethodSource("optimizations")
    void shouldCorrectlyHandleExceptionsWhenOptimizationIsUsedWithLockBasedProxyManagerMock(OptimizationCase testCase) throws Exception {
        LockBasedProxyManagerMock proxyManagerMock = new LockBasedProxyManagerMock(ClientSideConfig.getDefault().withClientClock(CLOCK));
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();

        BucketProxy bucket = proxyManagerMock.builder()
            .withOptimization(testCase.optimization())
            .build("66", configuration);

        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume(1)).isTrue();
        }
        proxyManagerMock.removeProxy("66");

        bucket.forceAddTokens(90);
        assertThat(bucket.getAvailableTokens()).isEqualTo(100);

        proxyManagerMock.removeProxy("66");

        bucket.asVerbose().forceAddTokens(90);
        assertThat(bucket.asVerbose().getAvailableTokens().getValue()).isEqualTo(100);
    }

    @ParameterizedTest
    @MethodSource("optimizations")
    void shouldCorrectlyHandleExceptionsWhenOptimizationIsUsedWithSelectForUpdateBasedProxyManagerMock(OptimizationCase testCase) throws Exception {
        SelectForUpdateBasedProxyManagerMock proxyManagerMock = new SelectForUpdateBasedProxyManagerMock(ClientSideConfig.getDefault().withClientClock(CLOCK));
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofSeconds(1)))
            .build();

        BucketProxy bucket = proxyManagerMock.builder()
            .withOptimization(testCase.optimization())
            .build("66", configuration);

        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume(1)).isTrue();
        }
        proxyManagerMock.removeProxy("66");

        bucket.forceAddTokens(90);
        assertThat(bucket.getAvailableTokens()).isEqualTo(100);

        proxyManagerMock.removeProxy("66");

        bucket.asVerbose().forceAddTokens(90);
        assertThat(bucket.asVerbose().getAvailableTokens().getValue()).isEqualTo(100);
    }

    @ParameterizedTest
    @MethodSource("optimizations")
    void implicitConfigurationReplacementCaseForVersionIncrement(OptimizationCase testCase) throws Exception {
        ProxyManagerMock proxyManagerMock = new ProxyManagerMock(CLOCK);

        int KEY = 42;
        int PREVIOUS_VERSION = 1;
        Bucket bucket10 = proxyManagerMock.builder()
            .withOptimization(testCase.optimization())
            .withImplicitConfigurationReplacement(PREVIOUS_VERSION, TokensInheritanceStrategy.RESET)
            .build(KEY, BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
                .build());

        // persist bucket with previous version
        bucket10.tryConsumeAsMuchAsPossible();

        CommandExecutor executor = testCase.optimization().apply(new CommandExecutor() {
            @Override
            public <T> CommandResult<T> execute(RemoteCommand<T> command) {
                Request<T> request = new Request<>(command, Versions.getLatest(), CLOCK.currentTimeNanos(), null);
                return proxyManagerMock.execute(KEY, request);
            }
        });
        // emulate case where two command in parallel detects that config needs to be replaced
        RemoteCommand<?> getTokensCommand = new GetAvailableTokensCommand();
        CommandResult getTokensResult = executor.execute(new CheckConfigurationVersionAndExecuteCommand<>(getTokensCommand, PREVIOUS_VERSION + 1));
        RemoteCommand<?> syncCommand = new SyncCommand(1, 1_000_000);
        CommandResult syncResult = executor.execute(new CheckConfigurationVersionAndExecuteCommand<>(syncCommand, PREVIOUS_VERSION + 1));

        // then wrap original commands by CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand and repeat
        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(100).refillGreedy(10, Duration.ofSeconds(1)))
            .build();
        CommandResult syncResult2 = executor.execute(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, syncCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET));
        CommandResult getTokensResult2 = executor.execute(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, getTokensCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET));

        assertThat(getTokensResult.isConfigurationNeedToBeReplaced()).isTrue();
        assertThat(syncResult.isConfigurationNeedToBeReplaced()).isTrue();
        assertThat(syncResult2.isError()).isFalse();
        assertThat(getTokensResult2.getData()).isEqualTo(100L);
    }

}
