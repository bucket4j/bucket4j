package io.github.bucket4j.tck;

import java.util.function.Supplier;

import io.github.bucket4j.distributed.proxy.AbstractAsyncProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;

public class AsyncProxyManagerSpec<K, P extends AsyncProxyManager<K>, B extends AbstractAsyncProxyManagerBuilder<K, P, B>> {

    public final String description;
    public final Supplier<AbstractAsyncProxyManagerBuilder<K, P, B>> builder;
    public final Supplier<K> keyGenerator;
    public final boolean expirationSupported;

    public AsyncProxyManagerSpec(String description, boolean expirationSupported, Supplier<K> keyGenerator, Supplier<AbstractAsyncProxyManagerBuilder<K, P, B>> builder) {
        this.description = description;
        this.expirationSupported = expirationSupported;
        this.keyGenerator = keyGenerator;
        this.builder = builder;
    }

    public AsyncProxyManagerSpec(String description, Supplier<K> keyGenerator, Supplier<AbstractAsyncProxyManagerBuilder<K, P, B>> builder) {
        this.description = description;
        this.expirationSupported = false;
        this.keyGenerator = keyGenerator;
        this.builder = builder;
    }

    public AsyncProxyManagerSpec<K, P , B> checkExpiration() {
        return new AsyncProxyManagerSpec<>(description, true, keyGenerator, builder);
    }

    @Override
    public String toString() {
        return "ProxyManagerSpec{" +
            "description='" + description + '\'' +
            '}';
    }

    public K generateRandomKey() {
        return keyGenerator.get();
    }
}
