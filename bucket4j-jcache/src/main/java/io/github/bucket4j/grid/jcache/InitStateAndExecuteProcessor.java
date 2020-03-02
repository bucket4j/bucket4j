
package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.GridCommand;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import javax.cache.processor.MutableEntry;
import java.io.IOException;
import java.io.Serializable;

public class InitStateAndExecuteProcessor<K extends Serializable, T extends Serializable> implements JCacheEntryProcessor<K, T> {

    private static final long serialVersionUID = 1L;

    private GridCommand<T> targetCommand;
    private BucketConfiguration configuration;

    public static SerializationHandle<InitStateAndExecuteProcessor<?, ?>> SERIALIZATION_HANDLE = new SerializationHandle<InitStateAndExecuteProcessor<?, ?>>() {
        @Override
        public <S> InitStateAndExecuteProcessor<?, ?> deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            GridCommand<?> targetCommand = (GridCommand<?>) adapter.readObject(input);
            BucketConfiguration configuration = adapter.readObject(input, BucketConfiguration.class);
            return new InitStateAndExecuteProcessor<>(targetCommand, configuration);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, InitStateAndExecuteProcessor<?, ?> processor) throws IOException {
            adapter.writeObject(output, processor.targetCommand);
            adapter.writeObject(output, processor.configuration);
        }

        @Override
        public int getTypeId() {
            return 19;
        }

        @Override
        public Class<InitStateAndExecuteProcessor<?, ?>> getSerializedType() {
            return (Class)InitStateAndExecuteProcessor.class;
        }

    };

    public InitStateAndExecuteProcessor(GridCommand<T> targetCommand, BucketConfiguration configuration) {
        this.configuration = configuration;
        this.targetCommand = targetCommand;
    }

    @Override
    public CommandResult<T> process(MutableEntry<K, GridBucketState> mutableEntry, Object... arguments) {
        boolean newStateCreated = false;
        long currentTimeNanos = currentTimeNanos();
        GridBucketState gridBucketState;
        if (mutableEntry.exists()) {
            gridBucketState = mutableEntry.getValue();
        } else {
            BucketState bucketState = BucketState.createInitialState(configuration, currentTimeNanos);
            gridBucketState = new GridBucketState(configuration, bucketState);
            newStateCreated = true;
        }

        T result = targetCommand.execute(gridBucketState, currentTimeNanos);
        if (newStateCreated || targetCommand.isBucketStateModified()) {
            mutableEntry.setValue(gridBucketState);
        }
        return CommandResult.success(result);
    }

    public GridCommand<T> getTargetCommand() {
        return targetCommand;
    }

    public BucketConfiguration getConfiguration() {
        return configuration;
    }

}
