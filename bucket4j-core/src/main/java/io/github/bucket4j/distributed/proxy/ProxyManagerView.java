package io.github.bucket4j.distributed.proxy;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;

public class ProxyManagerView<K, KeyOld> implements ProxyManager<K> {
    private final ProxyManager<KeyOld> target;
    private final Function<K, KeyOld> mapper;

    public ProxyManagerView(ProxyManager<KeyOld> target, Function<K, KeyOld> mapper) {
        this.target = target;
        this.mapper = mapper;
    }

    @Override
    public BucketProxy getProxy(K key, Supplier<BucketConfiguration> configurationSupplier) {
        return target.getProxy(mapper.apply(key), configurationSupplier);
    }

    @Override
    public RemoteBucketBuilder<K> builder() {
        return target.builder().withMapper(mapper);
    }

    @Override
    public Optional<BucketConfiguration> getProxyConfiguration(K key) {
        return target.getProxyConfiguration(mapper.apply(key));
    }

    @Override
    public void removeProxy(K key) {
        target.removeProxy(mapper.apply(key));
    }

    @Override
    public boolean isAsyncModeSupported() {
        return target.isAsyncModeSupported();
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return target.isExpireAfterWriteSupported();
    }

    @Override
    public AsyncProxyManager<K> asAsync() throws UnsupportedOperationException {
        return target.asAsync().withMapper(mapper);
    }

    // To prevent nesting of anonymous class instances, directly map the original instance.
    @Override
    public <K2> ProxyManager<K2> withMapper(Function<? super K2, ? extends K> innerMapper) {
        return target.withMapper(mapper.compose(innerMapper));
    }

}
