package io.github.bucket4j;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.Function;

/**
 * TODO write javadocs
 */
public class VerboseResult<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long operationTimeNanos;
    private final T value;
    private final BucketConfiguration configuration;
    private final BucketState state;

    public VerboseResult(long operationTimeNanos, T value, BucketConfiguration configuration, BucketState state) {
        this.operationTimeNanos = operationTimeNanos;
        this.value = value;
        this.configuration = configuration;
        this.state = state;
    }

    /**
     * TODO write javadocs
     */
    public T getValue() {
        return value;
    }

    /**
     * TODO write javadocs
     */
    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * TODO write javadocs
     */
    public BucketState getState() {
        return state;
    }

    /**
     * TODO write javadocs
     */
    public long getOperationTimeNanos() {
        return operationTimeNanos;
    }

    public <R extends Serializable> VerboseResult<R> map(Function<T, R> mapper) {
        return new VerboseResult<>(operationTimeNanos, mapper.apply(value), configuration, state);
    }

    public static SerializationHandle<VerboseResult<?>> SERIALIZATION_HANDLE = new SerializationHandle<VerboseResult<?>>() {

        @Override
        public <I> VerboseResult<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            long operationTimeNanos = adapter.readLong(input);
            Serializable result = (Serializable) adapter.readObject(input);
            BucketConfiguration configuration = adapter.readObject(input, BucketConfiguration.class);
            BucketState state = adapter.readObject(input, BucketState.class);

            return new VerboseResult<>(operationTimeNanos, result, configuration, state);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, VerboseResult<?> result) throws IOException {
            adapter.writeLong(output, result.operationTimeNanos);
            adapter.writeObject(output, result.value);
            adapter.writeObject(output, result.configuration);
            adapter.writeObject(output, result.state);
        }

        @Override
        public int getTypeId() {
            return 24;
        }

        @Override
        public Class<VerboseResult<?>> getSerializedType() {
            return (Class) VerboseResult.class;
        }
    };

}
