package io.github.bucket4j.hazelcast.serialization;

import com.hazelcast.nio.serialization.StreamSerializer;
import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.hazelcast.serialization.HazelcastSerializer;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import static org.junit.Assert.assertEquals;

public class CommandResultSerializerTest<T extends Serializable> extends SerializerTest<CommandResult<T>> {

    @Override
    protected StreamSerializer<CommandResult<T>> getSerializerUnderTest() {
        return (StreamSerializer) HazelcastSerializer.COMMAND_RESULT_SERIALIZER;
    }

    @Override
    protected void runAssertions(CommandResult original, CommandResult deserialized) {
        assertEquals(original.isBucketNotFound(), deserialized.isBucketNotFound());
        assertEquals(original.getData(), deserialized.getData());
    }

    @Test
    public void serializeCommandResult_withoutPayload() throws IOException {
        testSerialization(CommandResult.bucketNotFound());
    }

    @Test
    public void serializeCommandResult_withIntegerPayload() throws IOException {
        CommandResult<T> commandResult = (CommandResult<T>) CommandResult.success(42L);

        testSerialization(commandResult);
    }

    @Test
    public void serializeCommandResult_withStringPayload() throws IOException {
        CommandResult<T> commandResult = (CommandResult<T>) CommandResult.success("foo");

        testSerialization(commandResult);
    }
}
