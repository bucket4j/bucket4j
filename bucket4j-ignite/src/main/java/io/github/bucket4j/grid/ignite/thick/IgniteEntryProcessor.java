package io.github.bucket4j.grid.ignite.thick;

import java.io.Serial;
import java.io.Serializable;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import org.apache.ignite.cache.CacheEntryProcessor;

import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.Request;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

class IgniteEntryProcessor<K> implements Serializable, CacheEntryProcessor<K, byte[], byte[]> {

    @Serial
    private static final long serialVersionUID = 1;

    private final byte[] requestBytes;

    IgniteEntryProcessor(Request<?> request) {
        this.requestBytes = serializeRequest(request);
    }

    @Override
    public byte[] process(MutableEntry<K, byte[]> entry, Object... arguments) throws EntryProcessorException {
        return new AbstractBinaryTransaction(requestBytes) {
            @Override
            public boolean exists() {
                return entry.exists();
            }

            @Override
            protected byte[] getRawState() {
                return entry.getValue();
            }

            @Override
            protected void setRawState(byte[] newStateBytes, RemoteBucketState newState) {
                entry.setValue(newStateBytes);
            }
        }.execute();
    }

}
