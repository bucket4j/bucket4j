package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;


public class SimpleBackupProcessor<K extends Serializable> implements EntryBackupProcessor<K, GridBucketState> {

    private static final long serialVersionUID = 1L;

    private final GridBucketState state;

    public SimpleBackupProcessor(GridBucketState state) {
        this.state = state;
    }

    @Override
    public void processBackup(Map.Entry<K, GridBucketState> entry) {
        entry.setValue(state);
    }

    public static SerializationHandle<SimpleBackupProcessor> SERIALIZATION_HANDLE = new SerializationHandle<SimpleBackupProcessor>() {

        @Override
        public <I> SimpleBackupProcessor deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            GridBucketState state = adapter.readObject(input, GridBucketState.class);
            return new SimpleBackupProcessor(state);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, SimpleBackupProcessor processor) throws IOException {
            adapter.writeObject(output, processor.state);
        }

        @Override
        public int getTypeId() {
            return 20;
        }

        @Override
        public Class<SimpleBackupProcessor> getSerializedType() {
            return SimpleBackupProcessor.class;
        }
    };

}
