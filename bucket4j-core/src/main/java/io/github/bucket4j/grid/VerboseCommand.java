package io.github.bucket4j.grid;

import io.github.bucket4j.VerboseResult;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.io.Serializable;

public class VerboseCommand<T extends Serializable> implements GridCommand<VerboseResult<T>> {

    private final GridCommand<T> targetCommand;

    public VerboseCommand(GridCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    @Override
    public VerboseResult<T> execute(GridBucketState state, long currentTimeNanos) {
        T result = targetCommand.execute(state, currentTimeNanos);
        return new VerboseResult<T>(currentTimeNanos, result, state.getConfiguration(), state.getState());
    }

    @Override
    public boolean isBucketStateModified() {
        return targetCommand.isBucketStateModified();
    }

    public GridCommand<T> getTargetCommand() {
        return targetCommand;
    }

    public static SerializationHandle<VerboseCommand<?>> SERIALIZATION_HANDLE = new SerializationHandle<VerboseCommand<?>>() {

        @Override
        public <I> VerboseCommand<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            GridCommand<?> targetCommand  = (GridCommand<?>) adapter.readObject(input);
            return new VerboseCommand<>(targetCommand);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, VerboseCommand<?> command) throws IOException {
            adapter.writeObject(output, command.targetCommand);
        }

        @Override
        public int getTypeId() {
            return 25;
        }

        @Override
        public Class<VerboseCommand<?>> getSerializedType() {
            return (Class) VerboseCommand.class;
        }
    };

}
