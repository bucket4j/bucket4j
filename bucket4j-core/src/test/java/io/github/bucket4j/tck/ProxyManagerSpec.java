package io.github.bucket4j.tck;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.github.bucket4j.distributed.proxy.AbstractProxyManagerBuilder;
import io.github.bucket4j.distributed.proxy.ProxyManager;

public class ProxyManagerSpec<K, P extends ProxyManager<K>, B extends AbstractProxyManagerBuilder<K, P, B>> {

    public final String description;
    public final Supplier<AbstractProxyManagerBuilder<K, P, B>> builder;
    public final Supplier<K> keyGenerator;
    public final boolean expirationSupported;
    public final BackwardCompatibilityStateCheckHelper<K> backwardCompatibilityStateCheckHelper;
    public final BackwardCompatibilityRequestCheckHelper<K> backwardCompatibilityRequestCheckHelper;

    private ProxyManagerSpec(String description, boolean expirationSupported,
                             BackwardCompatibilityStateCheckHelper<K> backwardCompatibilityStateCheckHelper,
                             BackwardCompatibilityRequestCheckHelper<K> backwardCompatibilityRequestCheckHelper,
                             Supplier<K> keyGenerator,
                             Supplier<AbstractProxyManagerBuilder<K, P, B>> builder) {
        this.description = description;
        this.expirationSupported = expirationSupported;
        this.keyGenerator = keyGenerator;
        this.builder = builder;
        this.backwardCompatibilityStateCheckHelper = backwardCompatibilityStateCheckHelper;
        this.backwardCompatibilityRequestCheckHelper = backwardCompatibilityRequestCheckHelper;
    }

    public ProxyManagerSpec(String description, Supplier<K> keyGenerator, Supplier<AbstractProxyManagerBuilder<K, P, B>> builder) {
        this.description = description;
        this.expirationSupported = false;
        this.keyGenerator = keyGenerator;
        this.builder = builder;
        this.backwardCompatibilityStateCheckHelper = new BackwardCompatibilityStateCheckHelper<K>() {
            @Override
            public byte[] getRawState(K key) {
                throw new IllegalStateException("You should explicitly call withoutBackwardCompatibilityChecker on ProxyManagerSpec" +
                        " if you want to avoid backward compatibility testing, " +
                        " however it is strongly recommended to configure checker via ProxyManagerSpec#checkStateBackwardCompatibility");
            }

            @Override
            public void setRawState(K key, byte[] state) {

            }
        };
        this.backwardCompatibilityRequestCheckHelper = new BackwardCompatibilityRequestCheckHelper<K>() {
            static String msg = "You should explicitly call withoutRequestCompatibilityStateChecker on ProxyManagerSpec." +
                    " Or you need to configure ProxyManagerSpec#checkRequestBackwardCompatibility if your proxy manager is implemented across any variant of Remote Procedure Call style like EntryProcessor or ComputeJob" +
                    " where request is being serialized to remote node";
            @Override
            public byte[] execute(K key, byte[] requestBytes) {
                throw new IllegalStateException(msg);
            }

            @Override
            public CompletableFuture<byte[]> executeAsync(K key, byte[] requestBytes) {
                throw new IllegalStateException(msg);
            }
        };
    }

    public ProxyManagerSpec<K, P , B> checkExpiration() {
        return new ProxyManagerSpec<>(description, true, backwardCompatibilityStateCheckHelper, backwardCompatibilityRequestCheckHelper, keyGenerator, builder);
    }

    public ProxyManagerSpec<K, P , B> checkStateBackwardCompatibility(BackwardCompatibilityStateCheckHelper<K> backwardCompatibilityStateCheckHelper) {
        return new ProxyManagerSpec<>(description, expirationSupported, backwardCompatibilityStateCheckHelper, backwardCompatibilityRequestCheckHelper ,keyGenerator, builder);
    }

    public ProxyManagerSpec<K, P , B> withoutBackwardCompatibilityStateChecker() {
        return new ProxyManagerSpec<>(description, expirationSupported, null, backwardCompatibilityRequestCheckHelper, keyGenerator, builder);
    }

    public ProxyManagerSpec<K, P , B> checkRequestBackwardCompatibility(BackwardCompatibilityRequestCheckHelper<K> backwardCompatibilityRequestCheckHelper) {
        return new ProxyManagerSpec<>(description, expirationSupported, backwardCompatibilityStateCheckHelper, backwardCompatibilityRequestCheckHelper ,keyGenerator, builder);
    }

    public ProxyManagerSpec<K, P , B> withoutBackwardCompatibilityRequestChecker() {
        return new ProxyManagerSpec<>(description, expirationSupported, backwardCompatibilityStateCheckHelper, null, keyGenerator, builder);
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
