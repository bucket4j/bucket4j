package io.github.bucket4j.distributed.proxy.optimization

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.TokensInheritanceStrategy
import io.github.bucket4j.distributed.BucketProxy
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor
import io.github.bucket4j.distributed.proxy.ClientSideConfig
import io.github.bucket4j.distributed.proxy.CommandExecutor
import io.github.bucket4j.distributed.proxy.optimization.delay.DelayOptimization
import io.github.bucket4j.distributed.proxy.optimization.manual.ManuallySyncingOptimization
import io.github.bucket4j.distributed.proxy.optimization.predictive.PredictiveOptimization
import io.github.bucket4j.distributed.proxy.optimization.skiponzero.SkipSyncOnZeroOptimization
import io.github.bucket4j.distributed.remote.CommandResult
import io.github.bucket4j.distributed.remote.RemoteCommand
import io.github.bucket4j.distributed.remote.Request
import io.github.bucket4j.distributed.remote.commands.CheckConfigurationVersionAndExecuteCommand
import io.github.bucket4j.distributed.remote.commands.CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand
import io.github.bucket4j.distributed.remote.commands.GetAvailableTokensCommand
import io.github.bucket4j.distributed.remote.commands.SyncCommand
import io.github.bucket4j.distributed.versioning.Versions
import io.github.bucket4j.mock.CompareAndSwapBasedProxyManagerMock
import io.github.bucket4j.mock.LockBasedProxyManagerMock

import io.github.bucket4j.mock.ProxyManagerMock
import io.github.bucket4j.mock.SelectForUpdateBasedProxyManagerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture

class OptimizationCornerCasesSpecification extends Specification {

    private static final DelayParameters delayParameters = new DelayParameters(1, Duration.ofNanos(1))

    @Shared
    private TimeMeterMock clock = new TimeMeterMock()


    // https://github.com/bucket4j/bucket4j/issues/398
    @Unroll
    def "should correctly handle exceptions when optimization is used #testNumber ProxyManagerMock"(int testNumber, Optimization optimization) {
        setup:
            ProxyManagerMock proxyManagerMock = new ProxyManagerMock(clock)
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            BucketProxy bucket = proxyManagerMock.builder()
                    .withOptimization(optimization)
                    .build("66", () -> configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90)
            bucket.asVerbose().getAvailableTokens().getValue() == 100

        where:
            [testNumber, optimization] << [
                [1, Optimizations.batching()],
                [2, new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [3, new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock)],
                [5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)]
        ]
    }

    // https://github.com/bucket4j/bucket4j/issues/398
    @Unroll
    def "should correctly handle exceptions when optimization is used #testNumber CompareAndSwapBasedProxyManagerMock"(int testNumber, Optimization optimization) {
        setup:
            CompareAndSwapBasedProxyManagerMock proxyManagerMock = new CompareAndSwapBasedProxyManagerMock(ClientSideConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            BucketProxy bucket = proxyManagerMock.builder()
                    .withOptimization(optimization)
                    .build("66", () -> configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90)
            bucket.asVerbose().getAvailableTokens().getValue() == 100

        where:
        [testNumber, optimization] << [
                [1, Optimizations.batching()],
                [2, new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [3, new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock)],
                [5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)]
        ]
    }

    // https://github.com/bucket4j/bucket4j/issues/398
    @Unroll
    def "should correctly handle exceptions when optimization is used #testNumber LockBasedProxyManagerMock"(int testNumber, Optimization optimization) {
        setup:
            LockBasedProxyManagerMock proxyManagerMock = new LockBasedProxyManagerMock(ClientSideConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            BucketProxy bucket = proxyManagerMock.builder()
                    .withOptimization(optimization)
                    .build("66", () -> configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90)
            bucket.asVerbose().getAvailableTokens().getValue() == 100

        where:
            [testNumber, optimization] << [
                    [1, Optimizations.batching()],
                    [2, new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                    [3, new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                    [4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock)],
                    [5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)]
            ]
    }

    @Unroll
    def "should correctly handle exceptions when optimization is used #testNumber SelectForUpdateBasedProxyManagerMock"(int testNumber, Optimization optimization) {
        setup:
            SelectForUpdateBasedProxyManagerMock proxyManagerMock = new SelectForUpdateBasedProxyManagerMock(ClientSideConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            BucketProxy bucket = proxyManagerMock.builder()
                .withOptimization(optimization)
                .build("66", () -> configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90)
            bucket.asVerbose().getAvailableTokens().getValue() == 100

        where:
            [testNumber, optimization] << [
                    [1, Optimizations.batching()],
                    [2, new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                    [3, new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                    [4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock)],
                    [5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)]
            ]
    }

    @Unroll
    def "#testNumber implicit configuration replacement case for version increment"(int testNumber, Optimization optimization) {
        when:
            ProxyManagerMock proxyManagerMock = new ProxyManagerMock(clock)

            int KEY = 42
            int PREVIOUS_VERSION = 1
            Bucket bucket10 = proxyManagerMock.builder()
                .withOptimization(optimization)
                .withImplicitConfigurationReplacement(PREVIOUS_VERSION, TokensInheritanceStrategy.RESET)
                .build(KEY, () -> BucketConfiguration.builder()
                    .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                    .build())

            // persist bucket with previous version
            bucket10.tryConsumeAsMuchAsPossible()

            CommandExecutor executor = optimization.apply(new CommandExecutor() {
                @Override
                CommandResult execute(RemoteCommand command) {
                    Request request = new Request(command, Versions.latest, clock.currentTimeNanos(), null)
                    return proxyManagerMock.execute(KEY, request)
                }
            })
            // emulate case where two command in parallel detects that config needs to be replaced
            RemoteCommand getTokensCommand = new GetAvailableTokensCommand()
            CommandResult getTokensResult = executor.execute(new CheckConfigurationVersionAndExecuteCommand<>(getTokensCommand, PREVIOUS_VERSION + 1))
            RemoteCommand syncCommand = new SyncCommand(1, 1_000_000)
            CommandResult syncResult = executor.execute(new CheckConfigurationVersionAndExecuteCommand<>(syncCommand, PREVIOUS_VERSION + 1))

            // then wrap original commands by CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand and repeat
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(100).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            CommandResult syncResult2 = executor.execute(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, syncCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET))
            CommandResult getTokensResult2 = executor.execute(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, getTokensCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET))
        then:
            getTokensResult.isConfigurationNeedToBeReplaced()
            syncResult.isConfigurationNeedToBeReplaced()
            !syncResult2.isError()
            getTokensResult2.getData() == 100L

        where:
            [testNumber, optimization] << [
                [1, Optimizations.batching()],
                [2, new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [3, new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock)],
                [5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)]
            ]
    }

    @Unroll
    def "#testNumber implicit configuration replacement case for version increment - Async"(int testNumber, Optimization optimization) {
        when:
            ProxyManagerMock proxyManagerMock = new ProxyManagerMock(clock)

            int KEY = 42
            int PREVIOUS_VERSION = 1
            Bucket bucket10 = proxyManagerMock.builder()
                    .withOptimization(optimization)
                    .withImplicitConfigurationReplacement(PREVIOUS_VERSION, TokensInheritanceStrategy.RESET)
                    .build(KEY, () -> BucketConfiguration.builder()
                            .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                            .build())

            // persist bucket with previous version
            bucket10.tryConsumeAsMuchAsPossible()

            AsyncCommandExecutor executor = optimization.apply (new AsyncCommandExecutor() {
                @Override
                CompletableFuture<CommandResult> executeAsync(RemoteCommand command) {
                    Request request = new Request(command, Versions.latest, clock.currentTimeNanos(), null)
                    return proxyManagerMock.executeAsync(KEY, request)
                }
            })
            // emulate case where two command in parallel detects that config needs to be replaced
            RemoteCommand getTokensCommand = new GetAvailableTokensCommand()
            CommandResult getTokensResult = executor.executeAsync(new CheckConfigurationVersionAndExecuteCommand<>(getTokensCommand, PREVIOUS_VERSION + 1)).get()
            RemoteCommand syncCommand = new SyncCommand(1, 1_000_000)
            CommandResult syncResult = executor.executeAsync(new CheckConfigurationVersionAndExecuteCommand<>(syncCommand, PREVIOUS_VERSION + 1)).get()

            // then wrap original commands by CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand and repeat
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit({limit -> limit.capacity(100).refillGreedy(10, Duration.ofSeconds(1))})
                    .build()
            CommandResult syncResult2 = executor.executeAsync(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, syncCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET)).get()
            CommandResult getTokensResult2 = executor.executeAsync(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, getTokensCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET)).get()
        then:
            getTokensResult.isConfigurationNeedToBeReplaced()
            syncResult.isConfigurationNeedToBeReplaced()
            !syncResult2.isError()
            getTokensResult2.getData() == 100L

        where:
            [testNumber, optimization] << [
                [1, Optimizations.batching()],
                [2, new DelayOptimization(delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [3, new PredictiveOptimization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeOptimizationListener.INSTANCE, clock)],
                [4, new SkipSyncOnZeroOptimization(NopeOptimizationListener.INSTANCE, clock)],
                [5, new ManuallySyncingOptimization(NopeOptimizationListener.INSTANCE, clock)]
            ]
    }

}
