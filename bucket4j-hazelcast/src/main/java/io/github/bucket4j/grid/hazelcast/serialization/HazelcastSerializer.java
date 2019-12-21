package io.github.bucket4j.grid.hazelcast.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.BucketState;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.serialization.Deserializer;
import io.github.bucket4j.serialization.SelfSerializable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class HazelcastSerializer<T extends SelfSerializable> implements StreamSerializer<T>, TypedStreamDeserializer<T> {

    public static HazelcastSerializer<Bandwidth> BANDWIDTH_SERIALIZER = new HazelcastSerializer<>(1, Bandwidth.class, Bandwidth.DESERIALIZER);
    public static HazelcastSerializer<BucketConfiguration> BUCKET_CONFIGURATION_SERIALIZER = new HazelcastSerializer<>(2, BucketConfiguration.class, BucketConfiguration.DESERIALIZER);
    public static HazelcastSerializer<BucketState> BUCKET_STATE_SERIALIZER = new HazelcastSerializer<>(3, BucketState.class, BucketState.DESERIALIZER);
    public static HazelcastSerializer<GridBucketState> GRID_BUCKET_STATE_SERIALIZER = new HazelcastSerializer<>(4, GridBucketState.class, GridBucketState.DESERIALIZER);
    public static HazelcastSerializer<CommandResult<?>> COMMAND_RESULT_SERIALIZER = new HazelcastSerializer<CommandResult<?>>(5, (Class) CommandResult.class, CommandResult.DESERIALIZER);

    public static List<HazelcastSerializer<?>> getAllSerializers(int typeIdBase) {
        return Arrays.asList(
                BANDWIDTH_SERIALIZER.withTypeId(++typeIdBase),
                BUCKET_CONFIGURATION_SERIALIZER.withTypeId(++typeIdBase),
                BUCKET_STATE_SERIALIZER.withTypeId(++typeIdBase),
                GRID_BUCKET_STATE_SERIALIZER.withTypeId(++typeIdBase),
                COMMAND_RESULT_SERIALIZER.withTypeId(++typeIdBase)
        );
    }

    private static ReadWriteBinding BINDING = new ReadWriteBinding();

    private final int typeId;
    private final Class<T> serializableType;
    private final Deserializer<T> deserializer;

    public HazelcastSerializer(int typeId, Class<T> serializableType, Deserializer<T> deserializer) {
        this.typeId = typeId;
        this.serializableType = serializableType;
        this.deserializer = deserializer;
    }

    public HazelcastSerializer<T> withTypeId(int typeId) {
        return new HazelcastSerializer<>(typeId, serializableType, deserializer);
    }

    public Class<T> getSerializableType() {
        return serializableType;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void write(ObjectDataOutput out, T selfSerializable) throws IOException {
        selfSerializable.serializeItself(BINDING, out);
    }

    @Override
    public T read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public T read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private T read0(ObjectDataInput in) throws IOException {
        return deserializer.deserialize(BINDING, in);
    }

}
