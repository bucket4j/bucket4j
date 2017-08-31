package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.function.SerializableFunction;

import java.io.Serializable;

/**
 * Created by vladimir.bukhtoyarov on 31.08.2017.
 */
class InfinispanEntryProcessorAdapter<K extends Serializable, T extends Serializable> implements SerializableFunction<EmbeddedCacheManager, CommandResult<T>> {

    private final JCacheEntryProcessor<K, T> entryProcessor;
    private final String cacheName;
    private final K key;

    public InfinispanEntryProcessorAdapter(JCacheEntryProcessor<K, T> entryProcessor, String cacheName, K key) {
        this.entryProcessor = entryProcessor;
        this.cacheName = cacheName;
        this.key = key;
    }

    @Override
    public CommandResult<T> apply(EmbeddedCacheManager embeddedCacheManager) {
        Cache<K, GridBucketState> cache = embeddedCacheManager.getCache(cacheName, false);

        // TODO

        //cache.computeIfAbsent();
        String result = entryProcessor.toString() + cacheName + key;
        return null;
    }
}
