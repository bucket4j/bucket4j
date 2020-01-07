package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.util.Map;

public class SimpleBackupProcessor<K> implements EntryBackupProcessor<K, RemoteBucketState> {

    private static final long serialVersionUID = 1L;

    private final RemoteBucketState state;

    SimpleBackupProcessor(RemoteBucketState state) {
        this.state = state;
    }

    @Override
    public void processBackup(Map.Entry<K, RemoteBucketState> entry) {
        entry.setValue(state);
    }

    public static SerializationHandle<SimpleBackupProcessor> SERIALIZATION_HANDLE = new SerializationHandle<SimpleBackupProcessor>() {

        @Override
        public <I> SimpleBackupProcessor deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            RemoteBucketState state = RemoteBucketState.SERIALIZATION_HANDLE.deserialize(adapter, input);
            return new SimpleBackupProcessor(state);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, SimpleBackupProcessor processor) throws IOException {
            RemoteBucketState.SERIALIZATION_HANDLE.serialize(adapter, output, processor.state);
        }

        @Override
        public int getTypeId() {
            return 25;
        }

        @Override
        public Class<SimpleBackupProcessor> getSerializedType() {
            return SimpleBackupProcessor.class;
        }

    };

}
