package io.github.bucket4j;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;
import io.github.bucket4j.grid.GridBucketState;

import java.io.IOException;

public class GridBucketStateSerializer implements StreamSerializer<GridBucketState>, TypedStreamDeserializer<GridBucketState> {

    private final int typeId;

    public GridBucketStateSerializer(int typeId) {
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return this.typeId;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void write(ObjectDataOutput out, GridBucketState gridBucketState) throws IOException {
        out.writeObject(gridBucketState.getConfiguration());
        out.writeObject(gridBucketState.getState());
    }

    @Override
    public GridBucketState read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public GridBucketState read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private GridBucketState read0(ObjectDataInput in) throws IOException {
        BucketConfiguration bucketConfiguration = in.readObject(BucketConfiguration.class);
        BucketState bucketState = in.readObject(BucketState.class);
        return new GridBucketState(bucketConfiguration, bucketState);
    }
}
