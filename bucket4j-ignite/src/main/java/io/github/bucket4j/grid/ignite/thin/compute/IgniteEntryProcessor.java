package io.github.bucket4j.grid.ignite.thin.compute;

import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.Request;
import org.apache.ignite.cache.CacheEntryProcessor;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

public class IgniteEntryProcessor<K> implements Serializable, CacheEntryProcessor<K, byte[], byte[]> {

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
            protected void setRawState(byte[] stateBytes) {
                entry.setValue(stateBytes);
            }
        }.execute();
    }

}
