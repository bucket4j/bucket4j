package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultiCommand implements RemoteCommand<MultiResult>, ComparableByContent<MultiCommand> {

    private List<RemoteCommand<?>> commands;

    public static SerializationHandle<MultiCommand> SERIALIZATION_HANDLE = new SerializationHandle<MultiCommand>() {
        @Override
        public <S> MultiCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            int size = adapter.readInt(input);
            List<RemoteCommand<?>> results = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                RemoteCommand<?> result = RemoteCommand.deserialize(adapter, input);
                results.add(result);
            }
            return new MultiCommand(results);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, MultiCommand multiCommand) throws IOException {
            adapter.writeInt(output, multiCommand.commands.size());
            for (RemoteCommand<?> command : multiCommand.commands) {
                RemoteCommand.serialize(adapter, output, command);
            }
        }

        @Override
        public int getTypeId() {
            return 22;
        }

        @Override
        public Class<MultiCommand> getSerializedType() {
            return MultiCommand.class;
        }

    };

    public MultiCommand(List<RemoteCommand<?>> commands) {
        this.commands = commands;
    }

    @Override
    public CommandResult<MultiResult> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        RemoteBucketState startingState = mutableEntry.exists() ? mutableEntry.get() : null;
        BucketEntryWrapper entryWrapper = new BucketEntryWrapper(startingState);

        List<CommandResult<?>> singleResults = new ArrayList<>(commands.size());
        for (RemoteCommand<?> singleCommand : commands) {
            singleResults.add(singleCommand.execute(entryWrapper, currentTimeNanos));
        }

        if (entryWrapper.stateModified) {
            mutableEntry.set(entryWrapper.state);
        }

        return CommandResult.success(new MultiResult(singleResults), MultiResult.SERIALIZATION_HANDLE);
    }

    @Override
    public boolean isInitializationCommand() {
        for (RemoteCommand command : commands) {
            if (command.isInitializationCommand()) {
                return true;
            }
        }
        return false;
    }

    public List<RemoteCommand<?>> getCommands() {
        return commands;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(MultiCommand other) {
        if (commands.size() != other.commands.size()) {
            return false;
        }
        for (int i = 0; i < commands.size(); i++) {
            RemoteCommand<?> command1 = commands.get(i);
            RemoteCommand<?> command2 = other.commands.get(i);
            if (!ComparableByContent.equals(command1, command2)) {
                return false;
            }
        }
        return true;
    }

    private static class BucketEntryWrapper implements MutableBucketEntry {

        private RemoteBucketState state;
        private boolean stateModified;

        public BucketEntryWrapper(RemoteBucketState state) {
            this.state = state;
        }

        @Override
        public boolean exists() {
            return state != null;
        }

        @Override
        public void set(RemoteBucketState state) {
            this.state = Objects.requireNonNull(state);
            this.stateModified = true;
        }

        @Override
        public RemoteBucketState get() {
            if (state == null) {
                throw new IllegalStateException("'exists' must be called before 'get'");
            }
            return state;
        }

    }

}
