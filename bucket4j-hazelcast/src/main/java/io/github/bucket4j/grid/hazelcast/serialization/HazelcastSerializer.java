package io.github.bucket4j.grid.hazelcast.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;
import io.github.bucket4j.*;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.jcache.ExecuteProcessor;
import io.github.bucket4j.grid.jcache.InitStateAndExecuteProcessor;
import io.github.bucket4j.grid.jcache.InitStateProcessor;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class HazelcastSerializer<T> implements StreamSerializer<T>, TypedStreamDeserializer<T> {

    public static HazelcastSerializer<Bandwidth> BANDWIDTH_SERIALIZER = new HazelcastSerializer<>(1, Bandwidth.class, Bandwidth.SERIALIZATION_HANDLE);
    public static HazelcastSerializer<BucketConfiguration> BUCKET_CONFIGURATION_SERIALIZER = new HazelcastSerializer<>(2, BucketConfiguration.class, BucketConfiguration.SERIALIZATION_HANDLE);
    public static HazelcastSerializer<BucketState> BUCKET_STATE_SERIALIZER = new HazelcastSerializer<>(3, BucketState.class, BucketState.SERIALIZATION_HANDLE);
    public static HazelcastSerializer<GridBucketState> GRID_BUCKET_STATE_SERIALIZER = new HazelcastSerializer<>(4, GridBucketState.class, GridBucketState.SERIALIZATION_HANDLE);
    public static HazelcastSerializer<CommandResult<?>> COMMAND_RESULT_SERIALIZER = new HazelcastSerializer<CommandResult<?>>(5, (Class) CommandResult.class, CommandResult.SERIALIZATION_HANDLE);
    public static HazelcastSerializer<EstimationProbe> ESTIMATION_PROBE_SERIALIZER = new HazelcastSerializer<>(6, EstimationProbe.class, EstimationProbe.SERIALIZATION_HANDLE);
    public static HazelcastSerializer<ConsumptionProbe> CONSUMPTION_PROBE_SERIALIZER = new HazelcastSerializer<>(7, ConsumptionProbe.class, ConsumptionProbe.SERIALIZATION_HANDLE);
    public static HazelcastSerializer<ExecuteProcessor<?, ?>> EXECUTE_PROCESSOR_SERIALIZER = new HazelcastSerializer<>(7, (Class) ExecuteProcessor.class, ExecuteProcessor.SERIALIZATION_HANDLE);
    public static HazelcastSerializer<InitStateProcessor> INIT_STATE_PROCESSOR_SERIALIZER = new HazelcastSerializer<>(7, (Class) InitStateProcessor.class, InitStateProcessor.SERIALIZATION_HANDLE);
    public static HazelcastSerializer<InitStateAndExecuteProcessor<?, ?>> INIT_STATE_AND_EXECUTE_PROCESSOR_SERIALIZER = new HazelcastSerializer<>(7, (Class) InitStateAndExecuteProcessor.class, InitStateAndExecuteProcessor.SERIALIZATION_HANDLE);

    public static List<HazelcastSerializer<?>> getAllSerializers(int typeIdBase) {
        return Arrays.asList(
                BANDWIDTH_SERIALIZER.withTypeId(++typeIdBase),
                BUCKET_CONFIGURATION_SERIALIZER.withTypeId(++typeIdBase),
                BUCKET_STATE_SERIALIZER.withTypeId(++typeIdBase),
                GRID_BUCKET_STATE_SERIALIZER.withTypeId(++typeIdBase),
                COMMAND_RESULT_SERIALIZER.withTypeId(++typeIdBase),

                ESTIMATION_PROBE_SERIALIZER.withTypeId(++typeIdBase),
                CONSUMPTION_PROBE_SERIALIZER.withTypeId(++typeIdBase),
                EXECUTE_PROCESSOR_SERIALIZER.withTypeId(++typeIdBase),
                INIT_STATE_PROCESSOR_SERIALIZER.withTypeId(++typeIdBase),
                INIT_STATE_AND_EXECUTE_PROCESSOR_SERIALIZER.withTypeId(++typeIdBase)
        );
    }

    private static ReadWriteBinding BINDING = new ReadWriteBinding();

    private final int typeId;
    private final Class<T> serializableType;
    private final SerializationHandle<T> serializationHandle;

    public HazelcastSerializer(int typeId, Class<T> serializableType, SerializationHandle<T> serializationHandle) {
        this.typeId = typeId;
        this.serializableType = serializableType;
        this.serializationHandle = serializationHandle;
    }

    public HazelcastSerializer<T> withTypeId(int typeId) {
        return new HazelcastSerializer<>(typeId, serializableType, serializationHandle);
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
    public void write(ObjectDataOutput out, T serializable) throws IOException {
        serializationHandle.serialize(BINDING, out, serializable);
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
        return serializationHandle.deserialize(BINDING, in);
    }

}
