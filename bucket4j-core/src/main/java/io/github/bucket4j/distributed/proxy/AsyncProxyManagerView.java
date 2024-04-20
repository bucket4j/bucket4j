package io.github.bucket4j.distributed.proxy;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.AsyncBucketProxy;

public class AsyncProxyManagerView<K, OldKey> implements AsyncProxyManager<K> {

    private final AsyncProxyManager<OldKey> target;
    private final Function<? super K, OldKey> mapper;

    public AsyncProxyManagerView(AsyncProxyManager<OldKey> target, Function<K, OldKey> mapper) {
        this.target = target;
        this.mapper = mapper;
    }

    @Override
    public AsyncBucketProxy getProxy(K key, Supplier<CompletableFuture<BucketConfiguration>> configurationSupplier) {
        return target.getProxy(mapper.apply(key), configurationSupplier);
    }

    @Override
    public RemoteAsyncBucketBuilder<K> builder() {
        return target.builder().withMapper(mapper);
    }

    @Override
    public CompletableFuture<Void> removeProxy(K key) {
        return target.removeProxy(mapper.apply(key));
    }

    @Override
    public CompletableFuture<Optional<BucketConfiguration>> getProxyConfiguration(K key) {
        return target.getProxyConfiguration(mapper.apply(key));
    }

    // To prevent nesting of anonymous class instances, directly map the original instance.
    @Override
    public <K2> AsyncProxyManager<K2> withMapper(Function<? super K2, ? extends K> innerMapper) {
        return target.withMapper(mapper.compose(innerMapper));
    }

}
