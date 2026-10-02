package io.github.bucket4j.grid.jcache;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;

import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.tck.BackwardCompatibilityRequestCheckHelper;

public class JCacheRequestCheckHelper implements BackwardCompatibilityRequestCheckHelper<String> {

    private final Cache<String, byte[]> cache;
    private final boolean preferLambdaStyle;

    public JCacheRequestCheckHelper(Cache<String, byte[]> cache, boolean preferLambdaStyle) {
        this.cache = cache;
        this.preferLambdaStyle = preferLambdaStyle;
    }

    @Override
    public byte[] execute(String key, byte[] requestBytes) {
        EntryProcessor<String, byte[], byte[]> processor = preferLambdaStyle ? createLambdaProcessor(requestBytes) : new RawRequestProcessor(requestBytes);
        return cache.invoke(key, processor);
    }

    @Override
    public CompletableFuture<byte[]> executeAsync(String key, byte[] requestBytes) {
        throw new UnsupportedOperationException();
    }

    private static EntryProcessor<String, byte[], byte[]> createLambdaProcessor(byte[] requestBytes) {
        return (Serializable & EntryProcessor<String, byte[], byte[]>) (mutableEntry, objects)
            -> new RawTransaction(mutableEntry, requestBytes).execute();
    }

    private static class RawRequestProcessor implements Serializable, EntryProcessor<String, byte[], byte[]> {

        @Serial
        private static final long serialVersionUID = 1;

        private final byte[] requestBytes;

        private RawRequestProcessor(byte[] requestBytes) {
            this.requestBytes = requestBytes;
        }

        @Override
        public byte[] process(MutableEntry<String, byte[]> mutableEntry, Object... arguments) {
            return new RawTransaction(mutableEntry, requestBytes).execute();
        }
    }

    private static class RawTransaction extends AbstractBinaryTransaction {

        private final MutableEntry<String, byte[]> targetEntry;

        private RawTransaction(MutableEntry<String, byte[]> targetEntry, byte[] requestBytes) {
            super(requestBytes);
            this.targetEntry = targetEntry;
        }

        @Override
        public boolean exists() {
            return targetEntry.exists();
        }

        @Override
        protected byte[] getRawState() {
            return targetEntry.getValue();
        }

        @Override
        protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
            targetEntry.setValue(newStateBytes);
        }
    }

}
