package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.util.ComparableByContent;
import org.infinispan.functional.EntryView;
import org.infinispan.util.function.SerializableFunction;

public class InfinispanProcessor<K, R> implements
        SerializableFunction<EntryView.ReadWriteEntryView<K, byte[]>, byte[]>,
        ComparableByContent<InfinispanProcessor> {

    private static final long serialVersionUID = 911L;

    private final byte[] requestBytes;

    public InfinispanProcessor(Request<R> request) {
        this.requestBytes = InternalSerializationHelper.serializeRequest(request);
    }

    public InfinispanProcessor(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    @Override
    public byte[] apply(EntryView.ReadWriteEntryView<K, byte[]> entry) {
        return new AbstractBinaryTransaction(requestBytes) {
            @Override
            public boolean exists() {
                return entry.find().isPresent();
            }

            @Override
            protected byte[] getRawState() {
                return entry.get();
            }

            @Override
            protected void setRawState(byte[] stateBytes) {
                entry.set(stateBytes);
            }
        }.execute();
    }

    @Override
    public boolean equalsByContent(InfinispanProcessor other) {
        return ComparableByContent.equals(requestBytes, other.requestBytes);
    }

    public byte[] getRequestBytes() {
        return requestBytes;
    }

}
