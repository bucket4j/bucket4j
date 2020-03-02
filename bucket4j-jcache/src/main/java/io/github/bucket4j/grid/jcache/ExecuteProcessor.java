package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import javax.cache.processor.MutableEntry;
import java.io.IOException;
import java.io.Serializable;

public class ExecuteProcessor<K extends Serializable, T extends Serializable> implements JCacheEntryProcessor<K, T> {

    private static final long serialVersionUID = 1;

    private GridCommand<T> targetCommand;

    public ExecuteProcessor(GridCommand<T> targetCommand) {
        this.targetCommand = targetCommand;
    }

    public static SerializationHandle<ExecuteProcessor<?, ?>> SERIALIZATION_HANDLE = new SerializationHandle<ExecuteProcessor<?, ?>>() {
        @Override
        public <S> ExecuteProcessor<?, ?> deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            GridCommand<?> targetCommand = (GridCommand<?>) adapter.readObject(input);
            return new ExecuteProcessor<>(targetCommand);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ExecuteProcessor<?, ?> processor) throws IOException {
            adapter.writeObject(output, processor.targetCommand);
        }

        @Override
        public int getTypeId() {
            return 17;
        }

        @Override
        public Class<ExecuteProcessor<?, ?>> getSerializedType() {
            return (Class) ExecuteProcessor.class;
        }

    };

    @Override
    public CommandResult<T> process(MutableEntry<K, GridBucketState> mutableEntry, Object... arguments) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }
        long currentTimeNanos = currentTimeNanos();
        GridBucketState gridBucketState = mutableEntry.getValue();

        T result = targetCommand.execute(gridBucketState, currentTimeNanos);
        if (targetCommand.isBucketStateModified()) {
            mutableEntry.setValue(gridBucketState);
        }
        return CommandResult.success(result);
    }

    public GridCommand<T> getTargetCommand() {
        return targetCommand;
    }

}
