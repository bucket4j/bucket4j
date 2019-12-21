package io.github.bucket4j;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.hazelcast.nio.serialization.TypedStreamDeserializer;
import io.github.bucket4j.grid.CommandResult;

import java.io.IOException;
import java.io.Serializable;


public class CommandResultSerializer<T extends Serializable> implements StreamSerializer<CommandResult<T>>, TypedStreamDeserializer<CommandResult<T>> {

    private final int typeId;

    public CommandResultSerializer(int typeId) {
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
    public void write(ObjectDataOutput out, CommandResult<T> commandResult) throws IOException {
        out.writeBoolean(commandResult.isBucketNotFound());
        if (!commandResult.isBucketNotFound()) {
            out.writeObject(commandResult.getData());
        }
    }

    @Override
    public CommandResult<T> read(ObjectDataInput in) throws IOException {
        return read0(in);
    }

    @Override
    public CommandResult<T> read(ObjectDataInput in, Class aClass) throws IOException {
        return read0(in);
    }

    private CommandResult<T> read0(ObjectDataInput in) throws IOException {
        boolean isBucketNotFound = in.readBoolean();
        return isBucketNotFound
                ? CommandResult.bucketNotFound()
                : CommandResult.success(in.readObject());
    }
}
