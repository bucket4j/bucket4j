package io.github.bucket4j.grid.hazelcast;

import com.hazelcast.map.EntryProcessor;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.util.ComparableByContent;

import java.util.Arrays;
import java.util.Map;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeCommand;
import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeResult;

public class HazelcastEntryProcessor<K, T> implements EntryProcessor<K, byte[], byte[]>, ComparableByContent<HazelcastEntryProcessor> {

    private static final long serialVersionUID = 1L;

    private final byte[] commandBytes;
    private EntryProcessor<K, byte[], byte[]> backupProcessor;

    public HazelcastEntryProcessor(RemoteCommand<T> command) {
        this.commandBytes = serializeCommand(command);
    }

    public HazelcastEntryProcessor(byte[] commandBytes) {
        this.commandBytes = commandBytes;
    }

    @Override
    public byte[] process(Map.Entry<K, byte[]> entry) {
        HazelcastMutableEntryAdapter<K> entryAdapter = new HazelcastMutableEntryAdapter<>(entry);
        RemoteCommand<T> command = InternalSerializationHelper.deserializeCommand(commandBytes);
        CommandResult<T> result = command.execute(entryAdapter, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
        if (entryAdapter.isModified()) {
            byte[] stateBytes = entry.getValue();
            backupProcessor = new SimpleBackupProcessor<>(stateBytes);
        }
        return serializeResult(result);
    }

    @Override
    public EntryProcessor<K, byte[], byte[]> getBackupProcessor() {
        return backupProcessor;
    }

    public byte[] getCommandBytes() {
        return commandBytes;
    }

    @Override
    public boolean equalsByContent(HazelcastEntryProcessor other) {
        return Arrays.equals(commandBytes, other.commandBytes);
    }

}
