package io.github.bucket4j.tck;

import java.util.function.Supplier;

import io.github.bucket4j.distributed.proxy.ProxyManager;

/**
 * @author Vladimir Bukhtoyarov
 */
public class ProxyManagerSpec<K> {

    public final String description;
    public final ProxyManager<K> proxyManager;
    public final Supplier<K> keyGenerator;

    public ProxyManagerSpec(String description, Supplier<K> keyGenerator, ProxyManager<K> proxyManager) {
        this.description = description;
        this.keyGenerator = keyGenerator;
        this.proxyManager = proxyManager;
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
