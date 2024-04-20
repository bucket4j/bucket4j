package io.github.bucket4j.distributed.proxy;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;

public class RemoteAsyncBucketBuilderView<K, KeyOld> implements RemoteAsyncBucketBuilder<K> {
    private final RemoteAsyncBucketBuilder<KeyOld> target;
    private final Function<K, KeyOld> mapper;

    public RemoteAsyncBucketBuilderView(RemoteAsyncBucketBuilder<KeyOld> target, Function<K, KeyOld> mapper) {
        this.target = target;
        this.mapper = mapper;
    }

    @Override
    public RemoteAsyncBucketBuilder<K> withRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
        target.withRecoveryStrategy(recoveryStrategy);
        return this;
    }

    @Override
    public RemoteAsyncBucketBuilder<K> withOptimization(Optimization optimization) {
        target.withOptimization(optimization);
        return this;
    }

    @Override
    public RemoteAsyncBucketBuilder<K> withImplicitConfigurationReplacement(long desiredConfigurationVersion, TokensInheritanceStrategy tokensInheritanceStrategy) {
        target.withImplicitConfigurationReplacement(desiredConfigurationVersion, tokensInheritanceStrategy);
        return this;
    }

    @Override
    public RemoteAsyncBucketBuilder<K> withListener(BucketListener listener) {
        target.withListener(listener);
        return this;
    }

    @Override
    public AsyncBucketProxy build(K key, BucketConfiguration configuration) {
        return target.build(mapper.apply(key), configuration);
    }

    @Override
    public AsyncBucketProxy build(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier) {
        return target.build(mapper.apply(key), configurationSupplier);
    }

    // To prevent nesting of anonymous class instances, directly map the original instance.
    @Override
    public <K2> RemoteAsyncBucketBuilder<K2> withMapper(Function<? super K2, ? extends K> innerMapper) {
        return target.withMapper(mapper.compose(innerMapper));
    }
}
