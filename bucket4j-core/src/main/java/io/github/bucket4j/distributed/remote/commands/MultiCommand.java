package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.distributed.remote.*;
import io.github.bucket4j.distributed.serialization.DeserializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationAdapter;
import io.github.bucket4j.distributed.serialization.SerializationHandle;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.distributed.versioning.Versions;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.bucket4j.distributed.versioning.Versions.v_5_0_0;

public class MultiCommand implements RemoteCommand<MultiResult>, ComparableByContent<MultiCommand> {

    private List<RemoteCommand<?>> commands;

    public static SerializationHandle<MultiCommand> SERIALIZATION_HANDLE = new SerializationHandle<MultiCommand>() {
        @Override
        public <S> MultiCommand deserialize(DeserializationAdapter<S> adapter, S input, Version backwardCompatibilityVersion) throws IOException {
            int formatNumber = adapter.readInt(input);
            Versions.check(formatNumber, v_5_0_0, v_5_0_0);

            int size = adapter.readInt(input);
            List<RemoteCommand<?>> results = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                RemoteCommand<?> result = RemoteCommand.deserialize(adapter, input, backwardCompatibilityVersion);
                results.add(result);
            }
            return new MultiCommand(results);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, MultiCommand multiCommand, Version backwardCompatibilityVersion) throws IOException {
            adapter.writeInt(output, v_5_0_0.getNumber());

            adapter.writeInt(output, multiCommand.commands.size());
            for (RemoteCommand<?> command : multiCommand.commands) {
                RemoteCommand.serialize(adapter, output, command, backwardCompatibilityVersion);
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

        if (entryWrapper.isStateModified()) {
            mutableEntry.set(entryWrapper.get());
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

}
