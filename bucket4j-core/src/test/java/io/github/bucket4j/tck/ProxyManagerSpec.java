package io.github.bucket4j.tck;

import java.util.function.Supplier;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.ProxyManager;

public class ProxyManagerSpec<K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> {

    public final String description;
    public final Supplier<AbstractProxyManagerBuilder<K, P, B>> builder;
    public final Supplier<K> keyGenerator;
    public final boolean expirationSupported;
    public final BackwardCompatibilityStateCheckHelper<K> backwardCompatibilityStateCheckHelper;

    private ProxyManagerSpec(String description, boolean expirationSupported,
                             BackwardCompatibilityStateCheckHelper<K> backwardCompatibilityStateCheckHelper,
                             Supplier<K> keyGenerator,
                             Supplier<AbstractProxyManagerBuilder<K, P, B>> builder) {
        this.description = description;
        this.expirationSupported = expirationSupported;
        this.keyGenerator = keyGenerator;
        this.builder = builder;
        this.backwardCompatibilityStateCheckHelper = backwardCompatibilityStateCheckHelper;
    }

    public ProxyManagerSpec(String description, Supplier<K> keyGenerator, Supplier<AbstractProxyManagerBuilder<K, P, B>> builder) {
        this.description = description;
        this.expirationSupported = false;
        this.keyGenerator = keyGenerator;
        this.builder = builder;
        this.backwardCompatibilityStateCheckHelper = null;
    }

    public ProxyManagerSpec<K, P , B> checkExpiration() {
        return new ProxyManagerSpec<>(description, true, backwardCompatibilityStateCheckHelper, keyGenerator, builder);
    }

    public ProxyManagerSpec<K, P , B> checkStateBackwardCompatibility(BackwardCompatibilityStateCheckHelper<K> backwardCompatibilityStateCheckHelper) {
        return new ProxyManagerSpec<>(description, expirationSupported, backwardCompatibilityStateCheckHelper, keyGenerator, builder);
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
