package io.github.bucket4j.grid.coherence;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.util.ComparableByContent;

import java.util.Arrays;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeResult;

public class CoherenceProcessor<K, T> extends AbstractProcessor<K, byte[], byte[]> implements ComparableByContent {

    private static final long serialVersionUID = 1L;

    private final byte[] requestBytes;

    public CoherenceProcessor(Request<T> request) {
        this.requestBytes = InternalSerializationHelper.serializeRequest(request);
    }

    public CoherenceProcessor(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    public byte[] getRequestBytes() {
        return requestBytes;
    }

    @Override
    public byte[] process(InvocableMap.Entry<K, byte[]> entry) {
        return new AbstractBinaryTransaction(requestBytes) {
            @Override
            public boolean exists() {
                return entry.getValue() != null;
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


    @Override
    public boolean equalsByContent(ComparableByContent other) {
        CoherenceProcessor processor = (CoherenceProcessor) other;
        return Arrays.equals(requestBytes, processor.requestBytes);
    }

}
