package io.github.bucket4j;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;

import java.io.IOException;

public class BucketStateSerializer implements StreamSerializer<BucketState>, TypedStreamDeserializer<BucketState> {

    private final int typeId;

    public BucketStateSerializer(int typeId) {
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
    public void write(ObjectDataOutput out, BucketState bucketState) throws IOException {
        out.writeLongArray(bucketState.stateData);
    }

    @Override
    public BucketState read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public BucketState read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private BucketState read0(ObjectDataInput in) throws IOException {
        long[] stateData = in.readLongArray();
        return new BucketState(stateData);
    }
}
