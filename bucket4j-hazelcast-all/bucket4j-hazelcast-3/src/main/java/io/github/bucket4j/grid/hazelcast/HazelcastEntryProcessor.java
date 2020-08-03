package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.distributed.remote.AbstractBinaryTransaction;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.util.ComparableByContent;

import java.util.Arrays;
import java.util.Map;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeRequest;

public class HazelcastEntryProcessor<K, T> implements EntryProcessor<K, byte[]>, ComparableByContent<HazelcastEntryProcessor> {

    private static final long serialVersionUID = 1L;

    private final byte[] requestBytes;
    private EntryBackupProcessor<K, byte[]> backupProcessor;

    public HazelcastEntryProcessor(Request<T> request) {
        this.requestBytes = serializeRequest(request);
    }

    public HazelcastEntryProcessor(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    @Override
    public byte[] process(Map.Entry<K, byte[]> entry) {
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
                backupProcessor = new SimpleBackupProcessor<>(stateBytes);
            }
        }.execute();
    }

    @Override
    public EntryBackupProcessor<K, byte[]> getBackupProcessor() {
        return backupProcessor;
    }

    public byte[] getRequestBytes() {
        return requestBytes;
    }

    @Override
    public boolean equalsByContent(HazelcastEntryProcessor other) {
        return Arrays.equals(requestBytes, other.requestBytes);
    }

}
