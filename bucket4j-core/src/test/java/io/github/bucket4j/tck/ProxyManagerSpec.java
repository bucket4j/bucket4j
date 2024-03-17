package io.github.bucket4j.tck;

import java.util.function.Function;
import java.util.function.Supplier;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;

/**
 * @author Vladimir Bukhtoyarov
 */
public class ProxyManagerSpec<K> {

    public final String description;
    public final Function<ClientSideConfig, ProxyManager<K>> proxyManagerSupplier;
    public final Supplier<K> keyGenerator;

    public ProxyManagerSpec(String description, Supplier<K> keyGenerator, Function<ClientSideConfig, ProxyManager<K>>  proxyManagerSupplier) {
        this.description = description;
        this.keyGenerator = keyGenerator;
        this.proxyManagerSupplier = proxyManagerSupplier;
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
