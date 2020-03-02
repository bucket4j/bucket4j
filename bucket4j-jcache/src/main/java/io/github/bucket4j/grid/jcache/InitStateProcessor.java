
package io.github.bucket4j.grid.jcache;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.Nothing;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import javax.cache.processor.MutableEntry;
import java.io.IOException;
import java.io.Serializable;

public class InitStateProcessor<K extends Serializable> implements JCacheEntryProcessor<K, Nothing> {

    private static final long serialVersionUID = 1;

    private BucketConfiguration configuration;

    public InitStateProcessor(BucketConfiguration configuration) {
        this.configuration = configuration;
    }

    public static SerializationHandle<InitStateProcessor<?>> SERIALIZATION_HANDLE = new SerializationHandle<InitStateProcessor<?>>() {
        @Override
        public <S> InitStateProcessor<?> deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            BucketConfiguration configuration = (BucketConfiguration) adapter.readObject(input, BucketConfiguration.class);
            return new InitStateProcessor<>(configuration);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, InitStateProcessor<?> processor) throws IOException {
            adapter.writeObject(output, processor.configuration);
        }

        @Override
        public int getTypeId() {
            return 18;
        }

        @Override
        public Class<InitStateProcessor<?>> getSerializedType() {
            return (Class) InitStateProcessor.class;
        }

    };

    @Override
    public CommandResult<Nothing> process(MutableEntry<K, GridBucketState> mutableEntry, Object... arguments) {
        if (mutableEntry.exists()) {
            return CommandResult.success(null);
        }
        long currentTimeNanos = currentTimeNanos();
        BucketState bucketState = BucketState.createInitialState(configuration, currentTimeNanos);
        GridBucketState gridBucketState = new GridBucketState(configuration, bucketState);
        mutableEntry.setValue(gridBucketState);
        return CommandResult.success(null);
    }

    public BucketConfiguration getConfiguration() {
        return configuration;
    }

}
