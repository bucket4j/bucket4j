package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class HazelcastEntryProcessor<K, T> implements EntryProcessor<K, RemoteBucketState>, ComparableByContent<HazelcastEntryProcessor> {

    private static final long serialVersionUID = 1L;

    private final RemoteCommand<T> command;
    private EntryBackupProcessor<K, RemoteBucketState> backupProcessor;

    public static SerializationHandle<HazelcastEntryProcessor> SERIALIZATION_HANDLE = new SerializationHandle<HazelcastEntryProcessor>() {

        @Override
        public <I> HazelcastEntryProcessor deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            RemoteCommand command = RemoteCommand.deserialize(adapter, input);;
            return new HazelcastEntryProcessor(command);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, HazelcastEntryProcessor processor) throws IOException {
            RemoteCommand.serialize(adapter, output, processor.command);
        }

        @Override
        public int getTypeId() {
            return 50;
        }

        @Override
        public Class<HazelcastEntryProcessor> getSerializedType() {
            return HazelcastEntryProcessor.class;
        }

    };

    public HazelcastEntryProcessor(RemoteCommand<T> command) {
        this.command = command;
    }

    @Override
    public Object process(Map.Entry<K, RemoteBucketState> entry) {
        HazelcastMutableEntryAdapter<K> entryAdapter = new HazelcastMutableEntryAdapter<>(entry);
        CommandResult<T> result = command.execute(entryAdapter, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
        if (entryAdapter.isModified()) {
            RemoteBucketState state = entry.getValue();
            backupProcessor = new SimpleBackupProcessor<>(state);
        }
        return result;
    }

    @Override
    public EntryBackupProcessor<K, RemoteBucketState> getBackupProcessor() {
        return backupProcessor;
    }

    @Override
    public boolean equalsByContent(HazelcastEntryProcessor other) {
        return ComparableByContent.equals(command, other.command);
    }

}
