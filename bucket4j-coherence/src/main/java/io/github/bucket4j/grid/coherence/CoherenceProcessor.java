package io.github.bucket4j.grid.coherence;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.util.ComparableByContent;

import java.util.Arrays;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeCommand;
import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.serializeResult;

public class CoherenceProcessor<K, T> extends AbstractProcessor<K, byte[], byte[]> implements ComparableByContent {

    private static final long serialVersionUID = 1L;

    private final byte[] commandBytes;

    public CoherenceProcessor(RemoteCommand<T> command) {
        this.commandBytes = InternalSerializationHelper.serializeCommand(command);
    }

    public CoherenceProcessor(byte[] commandBytes) {
        this.commandBytes = commandBytes;
    }

    public byte[] getCommandBytes() {
        return commandBytes;
    }

    @Override
    public byte[] process(InvocableMap.Entry<K, byte[]> entry) {
        CoherenceBackend.CoherenceEntry<K> entryAdapter = new CoherenceBackend.CoherenceEntry<>(entry);
        RemoteCommand<Object> command = deserializeCommand(commandBytes);
        CommandResult<?> result = command.execute(entryAdapter, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
        return serializeResult(result);
    }


    @Override
    public boolean equalsByContent(ComparableByContent other) {
        CoherenceProcessor processor = (CoherenceProcessor) other;
        return Arrays.equals(commandBytes, processor.commandBytes);
    }

}
