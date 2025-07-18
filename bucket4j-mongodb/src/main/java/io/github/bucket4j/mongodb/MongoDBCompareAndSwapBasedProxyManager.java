package io.github.bucket4j.mongodb;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ExpiredEntriesCleaner;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;

public class MongoDBCompareAndSwapBasedProxyManager <K> extends AbstractCompareAndSwapBasedProxyManager<K> implements ExpiredEntriesCleaner {
    protected MongoDBCompareAndSwapBasedProxyManager(ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
    }

    @Override
    public int removeExpired(int batchSize) {
        return 0;
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(K key) {
        return null;
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(K key) {
        return null;
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        return null;
    }

    @Override
    public void removeProxy(K key) {

    }

    @Override
    public boolean isAsyncModeSupported() {
        return false;
    }
}
