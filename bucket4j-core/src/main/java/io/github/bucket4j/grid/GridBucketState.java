
package io.github.bucket4j.grid;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.serialization.SerializationAdapter;

import java.io.IOException;
import java.io.Serializable;

public class GridBucketState implements Serializable {

    private static final long serialVersionUID = 1L;

    private BucketConfiguration configuration;
    private BucketState state;

    public static SerializationHandle<GridBucketState> SERIALIZATION_HANDLE = new SerializationHandle<GridBucketState>() {
        @Override
        public <S> GridBucketState deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            BucketConfiguration bucketConfiguration = adapter.readObject(input, BucketConfiguration.class);
            BucketState bucketState = adapter.readObject(input, BucketState.class);
            return new GridBucketState(bucketConfiguration, bucketState);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, GridBucketState gridState) throws IOException {
            adapter.writeObject(output, gridState.configuration);
            adapter.writeObject(output, gridState.state);
        }

        @Override
        public int getTypeId() {
            return 4;
        }

        @Override
        public Class<GridBucketState> getSerializedType() {
            return GridBucketState.class;
        }

    };

    public GridBucketState(BucketConfiguration configuration, BucketState state) {
        this.configuration = configuration;
        this.state = state;
    }

    public GridBucketState deepCopy() {
        return new GridBucketState(configuration, state.copy());
    }

    public void refillAllBandwidth(long currentTimeNanos) {
        state.refillAllBandwidth(configuration.getBandwidths(), currentTimeNanos);
    }

    public long getAvailableTokens() {
        return state.getAvailableTokens(configuration.getBandwidths());
    }

    public void consume(long tokensToConsume) {
        state.consume(configuration.getBandwidths(), tokensToConsume);
    }

    public long calculateDelayNanosAfterWillBePossibleToConsume(long tokensToConsume, long currentTimeNanos) {
        return state.calculateDelayNanosAfterWillBePossibleToConsume(configuration.getBandwidths(), tokensToConsume, currentTimeNanos);
    }

    public void addTokens(long tokensToAdd) {
        state.addTokens(configuration.getBandwidths(), tokensToAdd);
    }

    public BucketState copyBucketState() {
        return state.copy();
    }

    public BucketConfiguration replaceConfigurationOrReturnPrevious(BucketConfiguration newConfiguration) {
        if (!configuration.isCompatible(newConfiguration)) {
            return configuration;
        }
        configuration = newConfiguration;
        return null;
    }

    public BucketConfiguration getConfiguration() {
        return configuration;
    }

    public BucketState getState() {
        return state;
    }

}
