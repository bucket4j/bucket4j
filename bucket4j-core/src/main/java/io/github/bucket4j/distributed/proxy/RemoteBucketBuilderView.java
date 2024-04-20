package io.github.bucket4j.distributed.proxy;

import java.util.function.Function;
import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketListener;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.optimization.Optimization;

public class RemoteBucketBuilderView<K, KeyOld> implements RemoteBucketBuilder<K> {
    private final RemoteBucketBuilder<KeyOld> target;
    private final Function<K, KeyOld> mapper;

    public RemoteBucketBuilderView(RemoteBucketBuilder<KeyOld> target, Function<K, KeyOld> mapper) {
        this.target = target;
        this.mapper = mapper;
    }

    @Override
    public RemoteBucketBuilder<K> withRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
        target.withRecoveryStrategy(recoveryStrategy);
        return this;
    }

    @Override
    public RemoteBucketBuilder<K> withOptimization(Optimization optimization) {
        target.withOptimization(optimization);
        return this;
    }

    @Override
    public RemoteBucketBuilder<K> withImplicitConfigurationReplacement(long desiredConfigurationVersion, TokensInheritanceStrategy tokensInheritanceStrategy) {
        target.withImplicitConfigurationReplacement(desiredConfigurationVersion, tokensInheritanceStrategy);
        return this;
    }

    @Override
    public RemoteBucketBuilder<K> withListener(BucketListener listener) {
        target.withListener(listener);
        return this;
    }

    @Override
    public BucketProxy build(K key, Supplier<BucketConfiguration> configurationSupplier) {
        return target.build(mapper.apply(key), configurationSupplier);
    }

    @Override
    public BucketProxy build(K key, BucketConfiguration configuration) {
        return target.build(mapper.apply(key), configuration);
    }

    // To prevent nesting of anonymous class instances, directly map the original instance.
    @Override
    public <K2> RemoteBucketBuilder<K2> withMapper(Function<? super K2, ? extends K> innerMapper) {
        return target.withMapper(mapper.compose(innerMapper));
    }
}
