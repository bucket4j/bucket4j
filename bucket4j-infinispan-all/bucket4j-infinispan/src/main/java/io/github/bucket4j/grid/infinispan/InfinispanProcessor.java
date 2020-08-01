package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.util.ComparableByContent;
import org.infinispan.functional.EntryView;
import org.infinispan.util.function.SerializableFunction;

public class InfinispanProcessor<K, R> implements
        SerializableFunction<EntryView.ReadWriteEntryView<K, byte[]>, byte[]>,
        ComparableByContent<InfinispanProcessor> {

    private static final long serialVersionUID = 911L;

    private final byte[] commandBytes;

    public InfinispanProcessor(RemoteCommand<R> command) {
        this.commandBytes = InternalSerializationHelper.serializeCommand(command);
    }

    public InfinispanProcessor(byte[] commandBytes) {
        this.commandBytes = commandBytes;
    }

    @Override
    public byte[] apply(EntryView.ReadWriteEntryView<K, byte[]> entryView) {
        InfinispanEntry<K> mutableEntry = new InfinispanEntry<>(entryView);
        RemoteCommand<R> command = InternalSerializationHelper.deserializeCommand(commandBytes);
        CommandResult<R> result = command.execute(mutableEntry, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
        return InternalSerializationHelper.serializeResult(result);
    }

    @Override
    public boolean equalsByContent(InfinispanProcessor other) {
        return ComparableByContent.equals(commandBytes, other.commandBytes);
    }

    public byte[] getCommandBytes() {
        return commandBytes;
    }

}
