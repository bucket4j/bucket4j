package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.util.ComparableByContent;
import org.infinispan.functional.EntryView;
import org.infinispan.util.function.SerializableFunction;

import java.io.IOException;
import java.io.Serializable;

public class InfinispanProcessor<K extends Serializable, R extends Serializable> implements
        SerializableFunction<EntryView.ReadWriteEntryView<K, RemoteBucketState>, CommandResult<R>>,
        ComparableByContent<InfinispanProcessor> {

    private static final long serialVersionUID = 42L;

    private final RemoteCommand<R> command;

    public InfinispanProcessor(RemoteCommand<R> command) {
        this.command = command;
    }

    @Override
    public CommandResult<R> apply(EntryView.ReadWriteEntryView<K, RemoteBucketState> entryView) {
        InfinispanBackend.InfinispanEntry<K> mutableEntry = new InfinispanBackend.InfinispanEntry<>(entryView);
        return command.execute(mutableEntry, TimeMeter.SYSTEM_MILLISECONDS.currentTimeNanos());
    }

    public static SerializationHandle<InfinispanProcessor> SERIALIZATION_HANDLE = new SerializationHandle<InfinispanProcessor>() {

        @Override
        public <I> InfinispanProcessor deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            RemoteCommand<?> command = RemoteCommand.deserialize(adapter, input);
            return new InfinispanProcessor(command);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, InfinispanProcessor processor) throws IOException {
            RemoteCommand.serialize(adapter, output, processor.command);
        }

        @Override
        public int getTypeId() {
            return 50;
        }

        @Override
        public Class<InfinispanProcessor> getSerializedType() {
            return InfinispanProcessor.class;
        }
    };

    @Override
    public boolean equalsByContent(InfinispanProcessor other) {
        return ComparableByContent.equals(command, other.command);
    }

}
