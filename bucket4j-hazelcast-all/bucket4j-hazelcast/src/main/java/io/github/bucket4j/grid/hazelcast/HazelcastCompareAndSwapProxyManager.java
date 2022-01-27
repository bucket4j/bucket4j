package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.IMap;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class HazelcastCompareAndSwapProxyManager<K> extends AbstractCompareAndSwapBasedProxyManager<K> {

    private final IMap<K, byte[]> map;

    public HazelcastCompareAndSwapProxyManager(IMap<K, byte[]> map) {
        this(map, ClientSideConfig.getDefault());
    }

    public HazelcastCompareAndSwapProxyManager(IMap<K, byte[]> map, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.map = Objects.requireNonNull(map);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncModeSupported() {
        // Because Hazelcast IMap does not provide "replaceAsync" API.
        return false;
    }

    @Override
    public void removeProxy(K key) {
        map.remove(key);
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData() {
                byte[] data = map.get(key);
                return Optional.ofNullable(data);
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData) {
                if (originalData == null) {
                    return map.putIfAbsent(key, newData) == null;
                } else {
                    return map.replace(key, originalData, newData);
                }
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        throw new UnsupportedOperationException();
    }

}
